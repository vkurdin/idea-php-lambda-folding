package vkurdin.idea.php.lambdafolding

import com.jetbrains.php.config.PhpLanguageLevel

class PhpOptionTypeProviderPHP70Test : PhpOptionTypeProviderPHP56Test() {
    override val phpLanguageLevel = PhpLanguageLevel.PHP700
}

