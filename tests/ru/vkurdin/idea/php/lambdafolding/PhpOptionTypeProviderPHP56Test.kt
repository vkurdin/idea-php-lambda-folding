package vkurdin.idea.php.lambdafolding

import com.intellij.codeInsight.completion.CompletionType
import com.jetbrains.php.config.PhpLanguageLevel
import com.jetbrains.php.config.PhpProjectConfigurationFacade
import ru.vkurdin.idea.php.lambdafolding.TestCase

open class PhpOptionTypeProviderPHP56Test : TestCase() {
    protected open val phpLanguageLevel = PhpLanguageLevel.PHP560

    override val testResourcesPath = "${super.testResourcesPath}/completions/phpOption"

    fun testMapCall() = assertCompletion("fooProperty")
    fun testMapChainedCall() = assertCompletion("fooBarProperty")

    fun testFilterCall() = assertCompletion("barProperty")
    fun testFilterChainedCall() = assertCompletion("barProperty")

    fun testFlatMapCall() = assertCompletion("fooBarProperty")

    override fun setUp() {
        super.setUp()

        PhpProjectConfigurationFacade.getInstance(myFixture.project).languageLevel = phpLanguageLevel

        myFixture.configureByFile(fileName)
        myFixture.complete(CompletionType.BASIC);
    }

    protected fun assertCompletion(completion: String) {
        assertTrue("Completions should contain \"${completion}\"", myFixture.lookupElementStrings?.contains(completion) ?: false)
    }
}