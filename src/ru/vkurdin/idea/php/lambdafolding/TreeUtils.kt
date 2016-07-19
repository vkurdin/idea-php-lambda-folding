package ru.vkurdin.idea.php.lambdafolding

import com.intellij.psi.PsiElement

fun PsiElement.prevSiblings() = siblingSeq { it.prevSibling }

fun PsiElement.nextSiblings() = siblingSeq { it.nextSibling }

inline private fun <T> T.siblingSeq(crossinline siblingProvider: (T) -> T?) =
    object : Sequence<T> {
        override fun iterator() =
            object : Iterator<T> {
                private var next: T? = siblingProvider(this@siblingSeq)

                override fun hasNext() = next != null

                override fun next() : T = next!!.let {
                    val current = it

                    next = siblingProvider(current)
                    current
                }
            }
    }

