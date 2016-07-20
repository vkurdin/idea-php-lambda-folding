package ru.vkurdin.idea.php.lambdafolding

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2

class PhpOptionTypeProvider : PhpTypeProvider2 {
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

    override fun getKey(): Char = '✈'

    override fun getBySignature(signature: String, project: Project): MutableCollection<out PhpNamedElement>? =
        if (signature.startsWith("\\PhpOption\\Option<")) {
            PhpIndex.getInstance(project).getClassesByFQN("\\PhpOption\\Option")
        } else {
            PhpIndex.getInstance(project).getClassesByFQN(signature)
        }

    override fun getType(element: PsiElement?) : String? =
        when (element) {
            is MethodReference ->
                element.classReference
                ?. let { classReference ->
                    when {
                        // Option::fromValue()
                        classReference is ClassReference &&
                        classReference.fqn in OPTION_CLASSES &&
                        element.name == "fromValue" ->
                            element.parameters
                            . letIf {
                                it.isNotEmpty() &&
                                it.first() is PhpTypedElement
                            }
                            ?. first()
                            ?. let { "\\PhpOption\\Option<${(it as PhpTypedElement).type.toString().replace("|", "#✈#")}>" }

                        // Option<T> -> filter, -> filterNot, -> forAll
                        element.name in TYPE_IMMUTABLE_FUNCTIONS -> classReference.type.getOptionalTypes()

                        // Option<T> -> map
                        element.name == "map" &&
                        classReference.type.hasOptionalTypes() ->
                            element.getPassedClosure()
                            ?. getLocalType(true) ?. types ?. asSequence()
                            ?. filterNot { it.startsWith("#✈\\PhpOption\\Option") }
                            ?. map { it.removePrefix("#✈") }
                            ?. joinToString("|")
                            ?. letIf { it.isNotBlank() }
                            ?. let { "\\PhpOption\\Option<${it.replace("|", "#✈#")}>" }

                        // Option<T> -> flatMap
                        element.name == "flatMap" &&
                        classReference.type.hasOptionalTypes() ->
                            element.getPassedClosure()
                            ?. getLocalType(true)
                            ?. getOptionalTypes()

                        else -> null
                    }
                }
            // Option<T> -> Function(function ($arg) { return $arg; })
            is Parameter ->
                element . letIf { it.firstChild !is ClassReference }
                ?. parent ?. letIs(ParameterList::class) ?. letIf {
                    it.parameters.isNotEmpty() &&
                    it.parameters.first() === element
                }
                ?. parent ?. letIf {
                    it is Function &&
                    it.isClosure
                }
                ?. parent ?. parent ?. parent ?. letIs(MethodReference::class)
                ?. classReference ?. type
                ?. getOptionalTypesSequence() ?. letIf { it.any() }
                ?. first()
                ?. let { it.substring(18, it.length - 1).replace("#✈#", "|") }

            else -> null
        }

    private fun PhpType.getOptionalTypes() : String? =
        this.getOptionalTypesSequence()
        . letIf { it.any() }
        ?. joinToString("|")

    private fun PhpType.getOptionalTypesSequence() : Sequence<String> =
        types.asSequence()
        . filter { it.startsWith("#✈\\PhpOption\\Option<") }
        . map { it.substring(2) }

    private fun PhpType.hasOptionalTypes() : Boolean = getOptionalTypesSequence().any()

    private fun MethodReference.getPassedClosure() : Function? =
        this.parameters . letIf { it.isNotEmpty() }
        ?. first() . letIs(PhpExpression::class)
        ?. firstChild . letIs(Function::class) ?. letIf { it.isClosure }

}