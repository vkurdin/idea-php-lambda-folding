package ru.vkurdin.idea.php.lambdafolding

import com.intellij.psi.PsiElement

fun prevSiblings(root: PsiElement) : Sequence<PsiElement> = siblingSeq(root, { it.prevSibling })

fun nextSiblings(root: PsiElement) : Sequence<PsiElement> = siblingSeq(root, { it.nextSibling })

inline private fun <T> siblingSeq(root: T, crossinline siblingProvider: (T) -> T?) =
    object : Sequence<T> {
        override fun iterator() =
            object : Iterator<T> {
                private var next: T? = siblingProvider(root)

                override fun hasNext() = next != null

                override fun next() : T = next!!.let {
                    val current = it
                    next = siblingProvider(current)
                    current
                }
            }
    }

