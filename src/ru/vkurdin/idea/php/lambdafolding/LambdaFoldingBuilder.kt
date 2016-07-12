package ru.vkurdin.idea.php.lambdafolding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil as IdeaUtils;
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import java.util.*

class LambdaFoldingBuilder : FoldingBuilderEx() {

    data class ClosureParts(
        val func: Function, // closure body
        val params: ParameterList, // parameters
        val use: PhpUseList?, // "use" construct
        val type: ClassReference?, // function return type(if any)
        val ret: PhpExpression // returned expression
    )
    
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<out FoldingDescriptor> {
        return IdeaUtils.findChildrenOfType(root, Function::class.java)
            .filter {
                // leave one-liner closures w/o errors
                it.isClosure &&
                document.getLineNumber(it.textRange.startOffset) == document.getLineNumber(it.textRange.endOffset) &&
                !IdeaUtils.hasErrorElements(it)
            }.map {
                val func = it;
                var params: ParameterList? = null
                var bodyStmts: GroupStatement? = null
                var use: PhpUseList? = null
                var type: ClassReference? = null

                func.children.forEach {
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
                            func,
                            params as ParameterList,
                            use,
                            type,
                            expression as PhpExpression
                        )
                    }
            }.filterNotNull()
            .flatMap {
                val foldGroup = FoldingGroup.newGroup("lambda_fold")
                var useVars = emptyArray<Variable>()

                if (it.use != null) {
                    useVars = it.use.children
                        .filterIsInstance(Variable::class.java)
                        .toTypedArray()
                }

                // hide "function", "return" keywords, semicolon
                listOf(
                    getFold(
                        it.func.node,
                        it.func.textRange.startOffset,
                        prevSiblings(it.params)
                            .filter { it is LeafPsiElement && it.text == "(" } // locate left parenthesis
                            .first()
                            .textRange
                            .startOffset
                        ,
                        "{ ",
                        foldGroup
                    ),
                    getFold(
                        it.func.node,
                            if (it.type == null) {
                                // no return type info
                                nextSiblings(if (useVars.size == 0) it.params else useVars.last()) // search start point depends on "use" presense
                                    .filter { it is LeafPsiElement && it.text == ")" } // locate right parenthesis
                                    .first()
                                    .textRange
                                    .endOffset
                            } else {
                                it.type.textRange.endOffset
                            }
                        ,
                        it.ret.textRange.startOffset,
                        " => ",
                        foldGroup
                    ),
                    getFold(
                        it.func.node,
                        it.ret.textRange.endOffset,
                        it.func.textRange.endOffset,
                        " }",
                        foldGroup
                    )
                )
            }.toTypedArray()
    }

    companion object Utils {
        @JvmStatic
        fun prevSiblings(root: PsiElement) : Sequence<PsiElement> {
            return siblingSeq(root, { it.prevSibling })
        }

        @JvmStatic
        fun nextSiblings(root: PsiElement) : Sequence<PsiElement> {
            return siblingSeq(root, { it.nextSibling })
        }

        @JvmStatic
        private fun <T> siblingSeq(root: T, siblingProvider: (T) -> T?) : Sequence<T> {
            return object : Sequence<T> {
                override fun iterator(): Iterator<T> {
                    return object: Iterator<T> {
                        private var next: T? = siblingProvider(root)

                        override fun hasNext(): Boolean {
                            return next != null
                        }

                        override fun next(): T {
                            if (next == null) {
                                throw NoSuchElementException()
                            }

                            val current = next
                            next = siblingProvider(current as T)

                            return current
                        }
                    }
                }
            }
        }
    }

    private fun getFold(node: ASTNode, rangeStart: Int, rangeEnd: Int, placeholder: String, group: FoldingGroup): FoldingDescriptor {
        return object : FoldingDescriptor(node, TextRange(rangeStart, rangeEnd), group) {
            override fun getPlaceholderText() = placeholder
        }
    }

    override fun getPlaceholderText(node: ASTNode): String? {
        return "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        return true;
    }
}