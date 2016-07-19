package ru.vkurdin.idea.php.lambdafolding

import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase

abstract class TestCase : LightPlatformCodeInsightFixtureTestCase() {
    protected open val testResourcesPath = "tests/data"

    protected val fileName: String
        get() = "${testResourcesPath}/${getTestName(true).substringBefore('_')}.php"

    override fun isWriteActionRequired() = false

    override fun getTestDataPath() = testResourcesPath
}