package jetlang.parser

sealed class AstNodeBase {
    // TODO: could we make this a function reference instead? to cutdown on boilerplate
    abstract fun accept(visitor: AstVisitor)
}

sealed class Statement : AstNodeBase()

class Print(val value: String) : Statement() {
    override fun accept(visitor: AstVisitor) = visitor.visitPrint(this)
}

class Out(val expression: Expression) : Statement() {
    override fun accept(visitor: AstVisitor) = visitor.visitOut(this)
}

abstract class Expression : AstNodeBase() {
    abstract fun stringContent(): String
}
