package com.rewiew.autofmt.twig

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class RewiewTwigPrintSpacingInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Twig print tag spacing"

    override fun getShortName(): String = "RewiewTwigPrintSpacing"

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val vf = file.virtualFile ?: return emptyArray()
        if (vf.extension?.lowercase() != "twig") return emptyArray()

        val text = file.text
        val problems = mutableListOf<ProblemDescriptor>()

        var i = 0
        var inComment = false
        var inVerbatim = false

        while (i < text.length - 1) {
            if (inComment) {
                if (text.startsWith("#}", i)) {
                    inComment = false
                    i += 2
                } else {
                    i++
                }
                continue
            }

            if (inVerbatim) {
                if (text.startsWith("{%", i)) {
                    val close = text.indexOf("%}", i + 2)
                    if (close == -1) break
                    val content = text.substring(i + 2, close).trim().lowercase()
                    if (content.startsWith("endverbatim")) {
                        inVerbatim = false
                    }
                    i = close + 2
                } else {
                    i++
                }
                continue
            }

            if (text.startsWith("{#", i)) {
                inComment = true
                i += 2
                continue
            }

            if (text.startsWith("{%", i)) {
                val close = text.indexOf("%}", i + 2)
                if (close == -1) break
                val content = text.substring(i + 2, close).trim().lowercase()
                if (content.startsWith("verbatim")) {
                    inVerbatim = true
                }
                i = close + 2
                continue
            }

            if (text.startsWith("{{", i)) {
                val close = text.indexOf("}}", i + 2)
                if (close == -1) break

                val tagText = text.substring(i, close + 2)
                if (needsSpacing(tagText)) {
                    val range = TextRange(i, close + 2)
                    val fix = RewiewTwigPrintSpacingFix(i, close + 2)
                    val descriptor = manager.createProblemDescriptor(
                        file,
                        range,
                        "Add spaces inside {{ }}",
                        ProblemHighlightType.WEAK_WARNING,
                        isOnTheFly,
                        fix
                    )
                    problems.add(descriptor)
                }

                i = close + 2
                continue
            }

            i++
        }

        return if (problems.isEmpty()) emptyArray() else problems.toTypedArray()
    }

    private fun needsSpacing(tagText: String): Boolean {
        if (!tagText.startsWith("{{") || !tagText.endsWith("}}")) return false
        if (tagText.length < 4) return false

        val afterOpen = tagText[2]
        val beforeClose = tagText[tagText.length - 3]

        val leftOk = afterOpen.isWhitespace() || afterOpen == '-'
        val rightOk = beforeClose.isWhitespace() || beforeClose == '-'

        return !(leftOk && rightOk)
    }

    private class RewiewTwigPrintSpacingFix(
        private val startOffset: Int,
        private val endOffset: Int
    ) : LocalQuickFix {
        override fun getName(): String = "Insert spaces inside {{ }}"

        override fun getFamilyName(): String = "Rewiew Twig formatting"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement?.containingFile ?: return
            val document = FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return
            val text = document.text

            if (startOffset < 0 || endOffset > text.length || startOffset >= endOffset) return
            val tagText = text.substring(startOffset, endOffset)
            if (!tagText.startsWith("{{") || !tagText.endsWith("}}")) return

            val updated = com.rewiew.autofmt.util.TwigAutoFixer.fixPrintTagSpacing(tagText)
            if (updated == tagText) return

            document.replaceString(startOffset, endOffset, updated)
        }
    }
}
