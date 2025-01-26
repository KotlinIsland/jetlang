package jetlang.parser

sealed class AstNodeBase {
    // TODO: could we make this a function reference instead? to cutdown on boilerplate
    abstract suspend fun accept(visitor: AstVisitor)
}

data class Program(val nodes: List<AstNodeBase>) : AstNodeBase() {
    override suspend fun accept(visitor: AstVisitor) = visitor.visitProgram(this)
}

sealed class Statement : AstNodeBase()


data class Print(val value: String) : Statement() {
    override suspend fun accept(visitor: AstVisitor) = visitor.visitPrint(this)
}

data class Out(val expression: Expression) : Statement() {
    override suspend fun accept(visitor: AstVisitor) = visitor.visitOut(this)
}

abstract class Expression : AstNodeBase() {
    abstract fun stringContent(): String
}
