package ru.vkurdin.idea.php.lambdafolding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil as IdeaUtils
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function

class LambdaFoldingBuilder : FoldingBuilderEx(), DumbAware {

    data class ClosureParts(
        val closure: Function, // closure body
        val params: ParameterList, // parameters
        val use: PhpUseList?, // "use" construct
        val returnType: ClassReference?, // function return type(if any)
        val expression: PhpExpression // returned expression
    )
    
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<out FoldingDescriptor> =
        IdeaUtils.findChildrenOfType(root, Function::class.java)
        .asSequence()
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
            ?.let { bodyStmts } // params and bodyStmts must be found
            ?.statements
            ?.asSequence()
            ?.filterIsInstance(Statement::class.java)
            ?.take(2) // take at most two statements
            ?.toList()
            ?.letIf { it.size == 1 } // closure body must contain exactly one ...
            ?.first() ?.letIs(PhpReturn::class.java) // ... return statement which result is ...
            ?.argument ?.letIs(PhpExpression::class.java) //  ...an arbitrary expression
            ?.let { expression ->
                ClosureParts(
                    closure,
                    params!!,
                    use,
                    type,
                    expression
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
            sequenceOf(
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
        }.toList()
        .toTypedArray()

    private fun getFold(node: ASTNode, range: TextRange, placeholder: String, group: FoldingGroup) =
        object : FoldingDescriptor(node, range, group) {
            override fun getPlaceholderText() = placeholder
        }

    override fun getPlaceholderText(node: ASTNode) = "..."

    override fun isCollapsedByDefault(node: ASTNode) = true
}