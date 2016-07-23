package ru.vkurdin.idea.php.lambdafolding

import kotlin.reflect.KClass

inline fun <T> T.letIf(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null

inline fun <T> T.letIfNot(predicate: (T) -> Boolean): T? = if (!predicate(this)) this else null

inline fun <T, reified R> T.letIs(klass: java.lang.Class<R>): R? = if (this is R) this else null

inline fun <T, reified R : Any> T.letIs(klass: KClass<R>): R? = if (this is R) this else null
