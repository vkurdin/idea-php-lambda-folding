package ru.vkurdin.idea.php.lambdafolding

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase

abstract class TestCase : LightPlatformCodeInsightFixtureTestCase() {
    protected val testResourcesPath = "tests/data"

    protected val fileName: String
        get() = "${testResourcesPath}/${getTestName(true)}.php"

    override fun isWriteActionRequired() = false

    override fun getTestDataPath() = testResourcesPath

    abstract protected fun doTest()
}