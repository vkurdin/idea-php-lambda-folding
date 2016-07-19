package ru.vkurdin.idea.php.lambdafolding

import com.jetbrains.php.config.PhpLanguageLevel
import com.jetbrains.php.config.PhpProjectConfigurationFacade

class LambdaFoldingBuilderTest : TestCase() {
    override val testResourcesPath = "${super.testResourcesPath}/folding"

    fun testLambdaFoldPHP5X_53() = doTest(PhpLanguageLevel.PHP530)
    fun testLambdaFoldPHP5X_54() = doTest(PhpLanguageLevel.PHP540)
    fun testLambdaFoldPHP5X_55() = doTest(PhpLanguageLevel.PHP550)
    fun testLambdaFoldPHP5X_56() = doTest(PhpLanguageLevel.PHP560)

    fun testLambdaFoldPHP70() = doTest(PhpLanguageLevel.PHP700)

    protected fun doTest(level : PhpLanguageLevel) {
        PhpProjectConfigurationFacade.getInstance(myFixture.project).languageLevel = level

        myFixture.configureByFile(fileName)
        myFixture.testFolding(fileName)
    }
}