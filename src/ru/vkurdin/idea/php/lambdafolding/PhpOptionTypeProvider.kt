package ru.vkurdin.idea.php.lambdafolding

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2

class PhpOptionTypeProvider : PhpTypeProvider2 {
    protected val OPTION_CLASSES = hashSetOf(
        "\\PhpOption\\Option",
        "\\PhpOption\\Some",
        "\\PhpOption\\None"
    )

    protected val IMMUTABLE_FUNCTIONS = hashSetOf(
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
                                    it.size > 0 &&
                                        it.first() is PhpTypedElement
                                }
                                ?. first()
                                ?. let { "\\PhpOption\\Option<${(it as PhpTypedElement).type.toString().replace("|", "#✈#")}>" }

                        // Option<T> -> filter, -> filterNot, -> forAll
                        (element as MethodReference).name in IMMUTABLE_FUNCTIONS ->
                            classReference.getOptionalTypes()

                        // Option<T> -> map
                        element.name == "map" && // TODO: flatMap
                        classReference.getOptionalTypesSequence().any() ->
                            element.parameters . letIf { it.size > 0 }
                                ?. first() . letIs(PhpExpression::class)
                                ?. firstChild . letIs(Function::class) ?. letIf { it.isClosure }
                                ?. getLocalType(true).toString()
                                . letIf { it.isNotBlank() }
                                ?. let { "\\PhpOption\\Option<${it.replace("|", "#✈#")}>" }

                        else -> null
                    }
                }
            // Option<T> -> Function(function ($arg) { return $arg; })
            is Parameter ->
                element . letIf { it.firstChild !is ClassReference }
                ?. parent ?. letIs(ParameterList::class) ?. letIf {
                    it.parameters.size > 0 &&
                    it.parameters.first() === element
                }
                ?. parent ?. letIf {
                    it is Function &&
                    it.isClosure
                }
                ?. parent ?. parent ?. parent ?. letIs(MethodReference::class)
                ?. classReference
                ?. getOptionalTypesSequence() ?. letIf {
                    it.any()
                }
                ?. first()
                ?. let {
                    it.substring(18, it.length - 1).replace("#✈#", "|")
                }

            else -> null
        }

    protected fun PhpTypedElement.getOptionalTypes() : String? =
        this.getOptionalTypesSequence()
        . letIf { it.any() }
        ?. joinToString("|")

    protected fun PhpTypedElement.getOptionalTypesSequence() : Sequence<String> =
        this.type.types.asSequence()
        . filter { it.startsWith("#✈\\PhpOption\\Option<") }
        . map { it.substring(2) }
}