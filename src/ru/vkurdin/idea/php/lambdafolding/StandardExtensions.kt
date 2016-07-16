package ru.vkurdin.idea.php.lambdafolding

inline fun <T> T.letIf(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null
inline fun <T, reified R> T.letIs(klass: java.lang.Class<R>): R? = if (this is R) this else null
