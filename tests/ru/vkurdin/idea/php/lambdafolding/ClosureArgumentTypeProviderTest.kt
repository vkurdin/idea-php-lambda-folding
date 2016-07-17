package ru.vkurdin.idea.php.lambdafolding

import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase.*

class ClosureArgumentTypeProviderTest : TestCase() {
    override val testResourcesPath = "${super.testResourcesPath}/completions"

    val defaultCompletion = "fooProperty"

    fun testArrayMap() = doTest()

    fun testNoCompletionForSpecifiedClass() {
        assertTrue("More specialized \\Bar should be used", myFixture.lookupElementStrings?.contains("barProperty") ?: false)
        assertFalse("\\Foo should be absent from completions", myFixture.lookupElementStrings?.contains("fooProperty") ?: true)
    }

    override fun setUp() {
        super.setUp()

        myFixture.configureByFile(fileName)
        myFixture.complete(CompletionType.BASIC, 1)
    }

    override fun doTest() {
        assertTrue(myFixture.lookupElementStrings?.contains(defaultCompletion) ?: false)
    }
}