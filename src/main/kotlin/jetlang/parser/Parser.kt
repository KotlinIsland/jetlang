package jetlang.parser

import com.copperleaf.kudzu.node.Node
import com.copperleaf.kudzu.node.NonTerminalNode
import com.copperleaf.kudzu.node.mapped.ValueNode
import com.copperleaf.kudzu.parser.Parser
import com.copperleaf.kudzu.parser.ParserContext
import com.copperleaf.kudzu.parser.ParserException
import com.copperleaf.kudzu.parser.chars.CharInParser
import com.copperleaf.kudzu.parser.chars.CharNotInParser
import com.copperleaf.kudzu.parser.chars.DigitParser
import com.copperleaf.kudzu.parser.chars.EndOfInputParser
import com.copperleaf.kudzu.parser.choice.PredictiveChoiceParser
import com.copperleaf.kudzu.parser.lazy.LazyParser
import com.copperleaf.kudzu.parser.many.ManyParser
import com.copperleaf.kudzu.parser.many.SeparatedByParser
import com.copperleaf.kudzu.parser.many.TimesParser
import com.copperleaf.kudzu.parser.mapped.FlatMappedParser
import com.copperleaf.kudzu.parser.mapped.MappedParser
import com.copperleaf.kudzu.parser.maybe.MaybeParser
import com.copperleaf.kudzu.parser.sequence.SequenceParser
import com.copperleaf.kudzu.parser.text.IdentifierTokenParser
import com.copperleaf.kudzu.parser.text.LiteralTokenParser
import com.copperleaf.kudzu.parser.text.OptionalWhitespaceParser
import com.copperleaf.kudzu.parser.text.RequiredWhitespaceParser
import java.math.BigDecimal

// helpers
val comma = CharInParser(',')
val space = RequiredWhitespaceParser()
val maybeSpace = OptionalWhitespaceParser()

infix fun <NodeType : Node, Return> Parser<NodeType>.mappedAs(block: ParserContext.(NodeType) -> Return) =
    MappedParser<NodeType, Return>(this, mapperFunction = block)

@Suppress("UNUSED") // TODO: it will be used
infix fun <NodeType : Node, Return : Node> Parser<NodeType>.toAst(block: ParserContext.(NodeType) -> Return) =
    FlatMappedParser<NodeType, Return>(this, mapperFunction = block)

infix fun <NodeType : Node, NextNode : Node, NextParser : Parser<NextNode>> Parser<NodeType>.space(
    next: NextParser
) =
    SequenceParser(this, RequiredWhitespaceParser(), next)

// using `run` here to get an inferred return type
fun programParser() = run {
    val expressionParser = LazyParser<ValueNode<Expression>>()

    fun functionParser(name: String, args: Int, lambdaArgs: Int): Parser<*> {
        return SequenceParser(
            LiteralTokenParser("$name("),
            TimesParser(
                SequenceParser(
                    expressionParser,
                    comma,
                ),
                args,
            ),
            TimesParser(
                SeparatedByParser(
                    IdentifierTokenParser(),
                    space,
                ),
                lambdaArgs,
            ),
            LiteralTokenParser("->"),
            space,
            expressionParser,
            CharInParser(')'),
        )
    }

    val expressionNotOperationParser = LazyParser<Node>()
    val operationParser = SequenceParser(
        expressionNotOperationParser,
        CharInParser('+', '-', '*', '/', '^'),
        expressionParser,
    )
    val sequenceParser = SequenceParser(
        CharInParser('{'),
        expressionParser,
        comma,
        expressionParser,
        CharInParser('}'),
    )

    val numberParser = SequenceParser(
        ManyParser(DigitParser()),
        MaybeParser(
            SequenceParser(
                CharInParser('.'),
                ManyParser(DigitParser()),
            )
        )
    ) mappedAs { NumberLiteral(BigDecimal(it.text)) }
    expressionNotOperationParser uses PredictiveChoiceParser(
        SequenceParser(
            CharInParser('('), expressionParser, CharInParser(')')
        ),
        IdentifierTokenParser(),
        sequenceParser,
        numberParser,
        functionParser("map", 1, 1),
        functionParser("reduce", 2, 2),
    )
    expressionParser uses (PredictiveChoiceParser(
        expressionNotOperationParser,
        operationParser,
    ) mappedAs { it.flatten() as Expression })
    val statementParser = PredictiveChoiceParser(
        SequenceParser(
            LiteralTokenParser("var"),
            space,
            IdentifierTokenParser(),
            maybeSpace,
            CharInParser('='),
            maybeSpace,
            expressionParser,
        ),
        LiteralTokenParser("out") space expressionParser mappedAs {
            Out(it.node3.flatten() as Expression)
        },
        SequenceParser(
            LiteralTokenParser("print"),
            space,
            CharInParser('"'),
            ManyParser(CharNotInParser('"')),
            CharInParser('"'),
        ) mappedAs { Print(it.node4.text) },
        expressionParser mappedAs { ExpressionStatement(it.value) }
    ) mappedAs { it.flatten() as Statement }
    SequenceParser(
        SeparatedByParser(
            statementParser,
            CharInParser('\n')
        ),
        EndOfInputParser()
    ) mappedAs {
        Program(it.node1.nodeList.map { node -> node.value })
    }
}

fun parseText(input: String): Result<Program> {
    val parser = programParser()
    val context = ParserContext.fromString(input)
    val (node, rest) = parser.runCatching {
        parse(context)
    }.getOrElse { return Result.failure(it) }

    if (!rest.isEmpty()) {
        return Result.failure(
            ParserException(
                "Expected end of input, but still had input remaining",
                parser = parser,
                input = context
            )
        )
    }
    return Result.success(node.value)
}

// TODO: this might not be needed when the entire parser includes Ast mappings
fun Node.flatten(): AstNodeBase =
    when (this) {
        is ValueNode<*> -> value as AstNodeBase
        is NonTerminalNode -> {
            require(children.size == 1)
            children[0].flatten()
        }

        else -> error("what is $this?")
    }
