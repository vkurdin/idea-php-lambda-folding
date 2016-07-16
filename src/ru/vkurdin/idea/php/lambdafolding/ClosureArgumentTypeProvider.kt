package ru.vkurdin.idea.php.lambdafolding

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.intellij.psi.util.PsiTreeUtil as TreeUtil
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3

class ClosureArgumentTypeProvider : PhpTypeProvider3 {
    override fun getKey() = '\u2002'

    override fun getBySignature(p0: String?, p1: Project?): MutableCollection<out PhpNamedElement>? = null

    override fun getType(element: PsiElement?): PhpType? {
        var closureParams : Array<out Parameter>? = null

        return element
            ?. letIs(Parameter::class.java)
            ?. parent
            ?. letIs(ParameterList::class.java)
            ?. parent
            ?. letIf { it is Function && it.isClosure }
            ?. let {
                closureParams = (it as Function).parameters
                it
            }
            ?. parent ?. parent ?. parent
            ?. letIs(FunctionReference::class.java)
            ?. getCallMetadata()
            ?. let { meta ->
                val index = closureParams!!.indexOf(element)

                if (index > -1 && index in meta.paramPositions) {
                    meta.types
                } else {
                    null
                }
            }
            ?. filter { it.length > 3 && it.endsWith("[]") }
            ?. map { it.substring(0, it.length - 2) }
            ?. letIf { it.any() }
            ?. let {
                val type = PhpType()
                it.forEach { strType -> type.add(strType) }
                type
            }
    }

    data class CallbackMetadata(val types : Sequence<String>, val paramPositions : Array<Int>)

    fun FunctionReference.getCallMetadata(): CallbackMetadata? =
        this.parameters
        .letIf { it.all { it is PhpExpression } }
        ?. let {
            val params = this.parameters.asSequence()

            when (this.name) {
                // array_map(callback(e), arr0, arr1, ...)
                "array_map" -> CallbackMetadata(
                    params.filterIndexed { i, e -> i >= 1 }.getTypes(),
                    arrayOf(0)
                )
                // array_filter(arr0, callback(e))
                "array_filter" -> CallbackMetadata(
                    params.take(1).getTypes(),
                    arrayOf(0)
                )
                // array_udiff(arr0, arr1, arr2 ... callback(e, e))
                "array_udiff" -> CallbackMetadata(
                    params.filterIndexed { i, e -> i < this.parameters.size - 1 }.getTypes(),
                    arrayOf(0, 1)
                )
                // array_reduce(arr, callback(carry, e))
                "array_reduce" -> CallbackMetadata(
                    params.take(1).getTypes(),
                    arrayOf(1)
                )
                // usort(arr, callback(e, e))
                "usort" -> CallbackMetadata(
                    params.take(1).getTypes(),
                    arrayOf(0, 1)
                )
                // uasort(arr, callback(e, e))
                "uasort" -> CallbackMetadata(
                    params.take(1).getTypes(),
                    arrayOf(0, 1)
                )
                else -> null
            }
        }

    private fun Sequence<PsiElement>.getTypes(): Sequence<String> =
        this.flatMap { (it as PhpExpression).type.types.asSequence() }
}