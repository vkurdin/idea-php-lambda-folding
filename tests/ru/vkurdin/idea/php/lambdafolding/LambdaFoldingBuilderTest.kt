package ru.vkurdin.idea.php.lambdafolding

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase

class LambdaFoldingBuilderTest : LightPlatformCodeInsightFixtureTestCase() {

    val testResourcesPath = "tests/data"

    private val fileName: String
        get() = "${getTestName(true)}.php"


    fun testLambdaFold() = doTest()


    override fun isWriteActionRequired() = false

    override fun getTestDataPath() = testResourcesPath

    private fun doTest() {
        val path = "$testResourcesPath/$fileName"

        myFixture.configureByFile(path)
        myFixture.testFolding(path)
    }
}