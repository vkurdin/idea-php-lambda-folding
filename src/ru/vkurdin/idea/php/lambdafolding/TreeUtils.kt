package ru.vkurdin.idea.php.lambdafolding

import com.intellij.psi.PsiElement

fun PsiElement.prevSiblings() = elementSeq { it.prevSibling }

fun PsiElement.nextSiblings() = elementSeq { it.nextSibling }

fun PsiElement.parents() = elementSeq { it.parent }

inline private fun <T> T.elementSeq(crossinline elementProvider: (T) -> T?) =
    object : Sequence<T> {
        override fun iterator() =
            object : Iterator<T> {
                private var next: T? = elementProvider(this@elementSeq)

                override fun hasNext() = next != null

                override fun next() : T = next!!.let {
                    val current = it

                    next = elementProvider(current)
                    current
                }
            }
    }

