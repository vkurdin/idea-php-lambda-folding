package ru.vkurdin.idea.php.lambdafolding

import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase.*

class ClosureArgumentTypeProviderTest : TestCase() {
    val defaultCompletion = "barProperty"

    fun testArrayMapComplete() = doTest()

    override fun doTest() {
        myFixture.configureByFile(fileName)
        myFixture.complete(CompletionType.BASIC, 1)

        assertTrue(myFixture.lookupElementStrings?.contains(defaultCompletion) ?: false)
    }
}