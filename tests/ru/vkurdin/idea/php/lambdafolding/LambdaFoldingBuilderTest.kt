package ru.vkurdin.idea.php.lambdafolding

class LambdaFoldingBuilderTest : TestCase() {

    fun testLambdaFold() = doTest()

    override fun doTest() {
        myFixture.configureByFile(fileName)
        myFixture.testFolding(fileName)
    }
}