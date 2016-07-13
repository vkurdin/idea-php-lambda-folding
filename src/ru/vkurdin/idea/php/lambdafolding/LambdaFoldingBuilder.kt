package ru.vkurdin.idea.php.lambdafolding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil as IdeaUtils
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function

class LambdaFoldingBuilder : FoldingBuilderEx() {

    data class ClosureParts(
        val closure: Function, // closure body
        val params: ParameterList, // parameters
        val use: PhpUseList?, // "use" construct
        val returnType: ClassReference?, // function return type(if any)
        val expression: PhpExpression // returned expression
    )
    
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<out FoldingDescriptor> {
        return IdeaUtils.findChildrenOfType(root, Function::class.java)
            .filter {
                // leave one-liner closures w/o errors
                it.isClosure &&
                document.getLineNumber(it.textRange.startOffset) == document.getLineNumber(it.textRange.endOffset) &&
                !IdeaUtils.hasErrorElements(it)
            }.map { closure ->
                var params: ParameterList? = null
                var bodyStmts: GroupStatement? = null
                var use: PhpUseList? = null
                var type: ClassReference? = null

                closure.children.forEach {
                    when (it) {
                        is ParameterList -> params = it
                        is GroupStatement -> bodyStmts = it
                        is PhpUseList -> use = it
                        is ClassReference -> type = it
                    }
                }

                params
                    ?.let { bodyStmts }
                    ?.let {
                        val returns = it.statements
                            .asSequence()
                            .filterIsInstance(Statement::class.java)
                            .take(2)
                            .toList()

                        if (returns.size == 1) returns.first() else null
                    }?.let {
                        if (it is PhpReturn && it.argument is PhpExpression) it.argument else null
                    }?.let { expression ->
                        ClosureParts(
                            closure,
                            params as ParameterList,
                            use,
                            type,
                            expression as PhpExpression
                        )
                    }
            }.filterNotNull()
            .flatMap { parts ->
                val foldGroup = FoldingGroup.newGroup("lambda_fold")
                var useVars = emptyList<Variable>()

                if (parts.use != null) {
                    useVars = parts.use.children.filterIsInstance(Variable::class.java)
                }

                // hide "function", "return" keywords, semicolon
                listOf(
                    getFold(
                        parts.closure.node,
                        TextRange(
                            parts.closure.textRange.startOffset,
                            prevSiblings(parts.params)
                                .filter { it is LeafPsiElement && it.text == "(" } // locate left parenthesis
                                .first()
                                .textRange.startOffset
                        ),
                        "{ ",
                        foldGroup
                    ),
                    getFold(
                        parts.closure.node,
                        TextRange(
                            if (parts.returnType == null) {
                                // no return type info
                                nextSiblings(if (useVars.size == 0) parts.params else useVars.last()) // search start point depends on "use" presense
                                    .filter { it is LeafPsiElement && it.text == ")" } // locate right parenthesis
                                    .first()
                                    .textRange.endOffset
                            } else {
                                parts.returnType.textRange.endOffset
                            },
                            parts.expression.textRange.startOffset
                        ),
                        " => ",
                        foldGroup
                    ),
                    getFold(
                        parts.closure.node,
                        TextRange(
                            parts.expression.textRange.endOffset,
                            parts.closure.textRange.endOffset
                        ),
                        " }",
                        foldGroup
                    )
                )
            }.toTypedArray()
    }

    private fun getFold(node: ASTNode, range: TextRange, placeholder: String, group: FoldingGroup) =
        object : FoldingDescriptor(node, range, group) {
            override fun getPlaceholderText() = placeholder
        }

    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun isCollapsedByDefault(node: ASTNode) = true

    companion object Utils {
        @JvmStatic
        fun prevSiblings(root: PsiElement) : Sequence<PsiElement> = siblingSeq(root, { it.prevSibling })

        @JvmStatic
        fun nextSiblings(root: PsiElement) : Sequence<PsiElement> = siblingSeq(root, { it.nextSibling })

        @JvmStatic
        private fun <T> siblingSeq(root: T, siblingProvider: (T) -> T?) =
            object : Sequence<T> {
                override fun iterator() =
                    object : Iterator<T> {
                        private var next: T? = siblingProvider(root)

                        override fun hasNext() = next != null

                        override fun next() : T = next!!.let {
                            val current = next
                            next = siblingProvider(current as T)
                            current
                        }
                    }
            }
    }
}