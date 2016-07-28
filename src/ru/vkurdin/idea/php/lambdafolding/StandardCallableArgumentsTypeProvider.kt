package ru.vkurdin.idea.php.lambdafolding

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.intellij.psi.util.PsiTreeUtil as TreeUtil
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2

class StandardCallableArgumentsTypeProvider : PhpTypeProvider2 {
    override fun getKey() = '✈'

    override fun getBySignature(signature: String, project: Project): MutableCollection<out PhpNamedElement>? =
        PhpIndex.getInstance(project).getClassesByFQN(signature)

    override fun getType(element: PsiElement?): String? =
        element
        ?. letIf {
            it is Parameter &&
            it.firstChild !is ClassReference
        }
        ?. parent ?. let { it as? ParameterList }
        ?. parent
        ?. letIs (Function::class) ?. letIf { it.isClosure }
        ?. let {
            val metadata =
                it.parent ?. parent ?. parent
                ?. let { it as? FunctionReference }
                ?. getCallMetadata()

            if (metadata ?. paramPositions ?. contains(it.parameters.indexOf(element)) ?: false)
                metadata ?. types
             else
                null
        }
        ?. filter { it.length > 3 && it.endsWith("[]") }
        ?. map { it.substring(0, it.length - 2) }
        ?. let {
            val type = PhpType()

            it.forEach { strType -> type.add(strType) }
            type.toString()
        }

    class CallbackMetadata(phpTypes : Sequence<PsiElement>, val paramPositions : Array<Int>) {
        val types = phpTypes.flatMap { (it as PhpExpression).type.types.asSequence() }
    }

    private fun FunctionReference.getCallMetadata(): CallbackMetadata? =
        this.parameters
        .letIf { it.all { it is PhpExpression } }
        ?. let {
            val params = this.parameters.asSequence()

            when (this.name) {
                // array_map(callback(e), arr0, arr1, ...)
                "array_map" -> CallbackMetadata(
                    params.filterIndexed { i, e -> i >= 1 },
                    arrayOf(0)
                )

                // array_filter(arr0, callback(e))
                "array_filter" -> CallbackMetadata(
                    params.take(1),
                    arrayOf(0)
                )

                // array_udiff(arr0, arr1, arr2 ... callback(e, e))
                "array_udiff" -> CallbackMetadata(
                    params.filterIndexed { i, e -> i < this.parameters.size - 1 },
                    arrayOf(0, 1)
                )

                // array_reduce(arr, callback(carry, e))
                "array_reduce" -> CallbackMetadata(
                    params.take(1),
                    arrayOf(1)
                )

                // usort(arr, callback(e, e))
                "usort" -> CallbackMetadata(
                    params.take(1),
                    arrayOf(0, 1)
                )

                // uasort(arr, callback(e, e))
                "uasort" -> CallbackMetadata(
                    params.take(1),
                    arrayOf(0, 1)
                )

                else -> null
            }
        }
}