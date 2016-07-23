package ru.vkurdin.idea.php.lambdafolding

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2

class PhpOptionTypeProvider : PhpTypeProvider2 {
    companion object {
        private val OPTION_CLASSES = hashSetOf(
            "\\PhpOption\\Option",
            "\\PhpOption\\Some",
            "\\PhpOption\\None"
        )

        private val TYPE_IMMUTABLE_FUNCTIONS = hashSetOf(
            "filter",
            "filterNot",
            "forAll"
        )

        private val INNER_TYPE_SEPARATOR = "#✈#"
    }

    override fun getKey() = '✈'

    override fun getBySignature(signature: String, project: Project): MutableCollection<out PhpNamedElement>? =
        if (signature.startsWith("\\PhpOption\\Option<")) {
            PhpIndex.getInstance(project).getClassesByFQN("\\PhpOption\\Option")
        } else {
            PhpIndex.getInstance(project).getClassesByFQN(signature)
        }

    override fun getType(element: PsiElement?) : String? =
        when (element) {
            is MethodReference -> element.inferOptionalCallType()
            is Parameter -> element.inferOptionalCallType()
            else -> null
        }

    private fun Parameter.inferOptionalCallType() =
        // Option<T> -> Function(function ($arg) { return $arg; })
        letIf { firstChild !is ClassReference }
        ?. parent ?. letIs(ParameterList::class)
        ?. letIf {
            it.parameters.isNotEmpty() &&
            it.parameters.first() === this
        }
        ?. parent
        ?. letIf {
            it is Function &&
            it.isClosure
        }
        ?. parent ?. parent ?. parent ?. letIs(MethodReference::class)
        ?. classReference ?. type
        ?. getOptionalTypesSequence()
        ?. letIf { it.any() }
        ?. first()
        ?. let { it.substring(18, it.length - 1).replace(INNER_TYPE_SEPARATOR, "|") }

    private fun MethodReference.inferOptionalCallType() =
        classReference
        ?. let {
            when {
                // Option::fromValue()
                it is ClassReference &&
                it.fqn in OPTION_CLASSES &&
                name == "fromValue"
                    -> inferStaticCallType()

                // Option<T> -> filter, -> filterNot, -> forAll
                name in TYPE_IMMUTABLE_FUNCTIONS
                    -> it.type.getOptionalTypes()

                // Option<T> -> map
                name == "map" &&
                it.type.hasOptionalTypes()
                    -> inferMapCallType()

                // Option<T> -> flatMap
                name == "flatMap" &&
                it.type.hasOptionalTypes()
                    -> inferFlatmapCallType()

                else -> null
            }
        }

    private fun MethodReference.inferFlatmapCallType() =
        getFirstPassedClosure()
        ?. getLocalType(true)
        ?. getOptionalTypes()

    private fun MethodReference.inferMapCallType() =
        getFirstPassedClosure()
        ?. getLocalType(true) ?. types ?. asSequence()
        ?. filterNot { it.startsWith("#✈\\PhpOption\\Option") }
        ?. map { it.removePrefix("#✈") }
        ?. joinToString(INNER_TYPE_SEPARATOR)
        ?. letIf { it.isNotBlank() }
        ?. let { "\\PhpOption\\Option<$it>" }

    private fun MethodReference.inferStaticCallType() =
        parameters
        . letIf {
            it.isNotEmpty() &&
            it.first() is PhpTypedElement
        }
        ?. first()
        ?. let { "\\PhpOption\\Option<${(it as PhpTypedElement).type.toString().replace("|", INNER_TYPE_SEPARATOR)}>" }

    private fun MethodReference.getFirstPassedClosure() : Function? =
        parameters
        . letIf { it.isNotEmpty() }
        ?. first() . letIs(PhpExpression::class)
        ?. firstChild . letIs(Function::class)
        ?. letIf { it.isClosure }

    private fun PhpType.getOptionalTypes() : String? =
        getOptionalTypesSequence()
        . letIf { it.any() }
        ?. joinToString("|")

    private fun PhpType.getOptionalTypesSequence() : Sequence<String> =
        types.asSequence()
        . filter { it.startsWith("#✈\\PhpOption\\Option<") }
        . map { it.substring(2) }

    private fun PhpType.hasOptionalTypes() : Boolean = getOptionalTypesSequence().any()
}