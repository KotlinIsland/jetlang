package jetlang.parser

import com.copperleaf.kudzu.node.Node
import com.copperleaf.kudzu.node.mapped.ValueNode
import com.copperleaf.kudzu.parser.Parser
import com.copperleaf.kudzu.parser.ParserContext
import com.copperleaf.kudzu.parser.ParserException
import com.copperleaf.kudzu.parser.chars.CharInParser
import com.copperleaf.kudzu.parser.chars.CharNotInParser
import com.copperleaf.kudzu.parser.chars.DigitParser
import com.copperleaf.kudzu.parser.chars.EndOfInputParser
import com.copperleaf.kudzu.parser.choice.PredictiveChoiceParser
import com.copperleaf.kudzu.parser.expression.ExpressionParser
import com.copperleaf.kudzu.parser.lazy.LazyParser
import com.copperleaf.kudzu.parser.many.ManyParser
import com.copperleaf.kudzu.parser.many.SeparatedByParser
import com.copperleaf.kudzu.parser.many.TimesParser
import com.copperleaf.kudzu.parser.mapped.FlatMappedParser
import com.copperleaf.kudzu.parser.mapped.MappedParser
import com.copperleaf.kudzu.parser.maybe.MaybeParser
import com.copperleaf.kudzu.parser.noop.NoopParser
import com.copperleaf.kudzu.parser.sequence.SequenceParser
import com.copperleaf.kudzu.parser.text.IdentifierTokenParser
import com.copperleaf.kudzu.parser.text.LiteralTokenParser
import com.copperleaf.kudzu.parser.text.OptionalWhitespaceParser
import com.copperleaf.kudzu.parser.text.RequiredWhitespaceParser
import com.copperleaf.kudzu.parser.expression.Operator as KudzuOperator

// helpers
val comma = CharInParser(',')
val space = CharInParser(' ')
val maybeSpace = MaybeParser(space)

infix fun <NodeType : Node, Return> Parser<NodeType>.mappedAs(block: ParserContext.(NodeType) -> Return) =
    MappedParser<NodeType, Return>(this, mapperFunction = block)

infix fun <NodeType : Node, NextNode : Node, NextParser : Parser<NextNode>> Parser<NodeType>.space(
    next: NextParser
) =
    SequenceParser(this, RequiredWhitespaceParser(), next)

// using `run` here to get an inferred return type
fun programParser() = run {
    val expressionParser = LazyParser<ValueNode<Expression>>()

    data class ParsedFunction(
        val args: List<Expression>,
        val lambdaArgs: List<String>,
        val lambda: Expression
    )

    fun functionParser(name: String, args: Int, lambdaArgs: Int) =
        SequenceParser(
            LiteralTokenParser("$name("),
            TimesParser(
                SequenceParser(
                    expressionParser,
                    maybeSpace,
                    comma,
                ),
                args,
            ) mappedAs { it.nodeList.map { node -> node.node1.value } },
            maybeSpace,
            SequenceParser(
                IdentifierTokenParser(),
                TimesParser(
                    SequenceParser(
                        space,
                        IdentifierTokenParser(),
                    ),
                    lambdaArgs - 1,
                )
            ) mappedAs {
                listOf(it.node1.text) + it.node2.nodeList.map { node -> node.node2.text }
            },
            space,
            LiteralTokenParser("->"),
            space,
            expressionParser,
            CharInParser(')'),
        ) mappedAs {
            ParsedFunction(
                args = it.node2.value,
                lambdaArgs = it.node4.value,
                lambda = it.node8.value
            )
        }

    val expressionNotOperationParser = LazyParser<ValueNode<Expression>>()
    val operationParserImpl = ExpressionParser(
        {
            SequenceParser(
                MaybeParser(maybeSpace),
                expressionNotOperationParser,
                maybeSpace
            ) mappedAs {
                it.node2.value
            }
        },
        KudzuOperator.Infix("+", 10) { left, right -> Operation(left, Operator.ADD, right) },
        KudzuOperator.Infix("-", 10) { left, right -> Operation(left, Operator.SUBTRACT, right) },
        KudzuOperator.Infix("*", 20) { left, right -> Operation(left, Operator.MULTIPLY, right) },
        KudzuOperator.Infix("/", 20) { left, right -> Operation(left, Operator.DIVIDE, right) },
        KudzuOperator.Infix("^", 30) { left, right -> Operation(left, Operator.EXPONENT, right) },
    )
    val operationParser = operationParserImpl mappedAs {
        operationParserImpl.evaluator.evaluate(it)
    }
    expressionNotOperationParser uses (PredictiveChoiceParser(
        SequenceParser(
            CharInParser('('), expressionParser, CharInParser(')')
        ) mappedAs { it.node2.value },
        SequenceParser(
            CharInParser('{'),
            expressionParser,
            comma,
            maybeSpace,
            expressionParser,
            CharInParser('}'),
        ) mappedAs { SequenceLiteral(it.node2.value, it.node5.value) },
        SequenceParser(
            ManyParser(DigitParser()),
            MaybeParser(
                SequenceParser(
                    CharInParser('.'),
                    ManyParser(DigitParser()),
                )
            )
        ) mappedAs { NumberLiteral(it.text.toBigDecimal()) },
        functionParser("map", 1, 1) mappedAs {
            MapJL(
                it.value.args[0],
                it.value.lambdaArgs[0],
                it.value.lambda
            )
        },
        functionParser("reduce", 2, 2) mappedAs {
            val function = it.value
            Reduce(
                function.args[0],
                function.args[1],
                function.lambdaArgs[0],
                function.lambdaArgs[1],
                function.lambda
            )
        },
        IdentifierTokenParser() mappedAs { Identifier(it.text) },
    ) mappedAs { (it.node as ValueNode<*>).value as Expression })
    expressionParser uses (PredictiveChoiceParser(
        operationParser,
        expressionNotOperationParser,
    ) mappedAs { (it.node as ValueNode<*>).value as Expression })

    val statementParser = PredictiveChoiceParser(
        SequenceParser(
            LiteralTokenParser("var"),
            space,
            IdentifierTokenParser(),
            maybeSpace,
            CharInParser('='),
            maybeSpace,
            expressionParser,
        ) mappedAs { Var(it.node3.text, it.node7.value) },
        LiteralTokenParser("out") space expressionParser mappedAs {
            Out(it.node3.value)
        },
        SequenceParser(
            LiteralTokenParser("print"),
            space,
            CharInParser('"'),
            ManyParser(CharNotInParser('"')),
            CharInParser('"'),
        ) mappedAs { Print(it.node4.text) },
        expressionParser mappedAs { ExpressionStatement(it.value) }
    ) mappedAs { (it.node as ValueNode<*>).value as Statement }
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
