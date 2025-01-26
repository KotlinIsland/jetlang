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
import com.copperleaf.kudzu.parser.text.RequiredWhitespaceParser
import com.copperleaf.kudzu.parser.value.CharLiteralParser

// helpers
val comma = CharLiteralParser(',')
val space = RequiredWhitespaceParser()

infix fun <NodeType : Node, Return> Parser<NodeType>.mappedAs(block: ParserContext.(NodeType) -> Return) =
    MappedParser<NodeType, Return>(this, mapperFunction = block)

@Suppress("UNUSED") // TODO: it will be used
infix fun <NodeType : Node, Return : Node> Parser<NodeType>.flatten(block: ParserContext.(NodeType) -> Return) =
    FlatMappedParser<NodeType, Return>(this, mapperFunction = block)

infix fun <NodeType : Node, NextNode : Node, NextParser : Parser<NextNode>> Parser<NodeType>.space(
    next: NextParser
) =
    SequenceParser(this, RequiredWhitespaceParser(), next)

// using `run` here to get an inferred return type
fun programParser() = run {
    val expressionParser = LazyParser<Node>()

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
            CharLiteralParser(')'),
        )
    }

    val expressionNotOperationParser = LazyParser<Node>()
    val operationParser = SequenceParser(
        expressionNotOperationParser,
        CharInParser('+', '-', '*', '/', '^'),
        expressionParser,
    )
    val sequenceParser = SequenceParser(
        CharLiteralParser('{'),
        expressionParser,
        comma,
        expressionParser,
        CharLiteralParser('}'),
    )

    val numberParser = SequenceParser(
        ManyParser(DigitParser()), MaybeParser(
            SequenceParser(
                CharLiteralParser('.'),
                ManyParser(DigitParser()),
            )
        )
    )
    expressionNotOperationParser uses PredictiveChoiceParser(
        SequenceParser(
            CharLiteralParser('('), expressionParser, CharLiteralParser(')')
        ),
        IdentifierTokenParser(),
        sequenceParser,
        numberParser,
        functionParser("map", 1, 1),
        functionParser("reduce", 2, 2),
    )
    expressionParser uses PredictiveChoiceParser(
        expressionNotOperationParser,
        operationParser,
    )
    val statementParser = PredictiveChoiceParser(
        SequenceParser(
            LiteralTokenParser("var"),
            space,
            IdentifierTokenParser(),
            CharLiteralParser('='),
            expressionParser,
        ),
        LiteralTokenParser("out") space expressionParser mappedAs { @Suppress("KotlinConstantConditions") // will be resolved
        Out(it.node3 as Expression); TODO("implement expressions in the AST") },
        SequenceParser(
            LiteralTokenParser("print"),
            space,
            CharInParser('"'),
            ManyParser(CharNotInParser('"')),
            CharInParser('"'),
        ) mappedAs { Print(it.node4.text) }
    )
    SequenceParser(
        SeparatedByParser(
            PredictiveChoiceParser(statementParser, expressionParser),
            CharInParser('\n')
        ),
        EndOfInputParser()
    ) mappedAs { Program(it.node1.children.map(Node::flatten)) }
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
