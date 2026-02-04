package com.rewiew.autofmt.twig

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.rewiew.autofmt.util.TwigAutoFixer

class RewiewTwigAttributeNamingInspection : LocalInspectionTool() {
    override fun getDisplayName(): String = "Twig id/class naming"

    override fun getShortName(): String = "RewiewTwigAttributeNaming"

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val vf = file.virtualFile ?: return emptyArray()
        if (vf.extension?.lowercase() != "twig") return emptyArray()

        val text = file.text
        val problems = mutableListOf<ProblemDescriptor>()

        val regex = Regex("\\b(class|id)=(['\\\"])([^'\\\"]+)\\2", RegexOption.IGNORE_CASE)
        regex.findAll(text).forEach { match ->
            val value = match.groupValues[3]
            if (value.contains("{") || value.contains("}")) return@forEach

            val tokens = value.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.isEmpty()) return@forEach

            val hasInvalid = tokens.any { !isKebabCase(it) }
            if (!hasInvalid) return@forEach

            val valueStart = match.range.first + match.groupValues[1].length + 2
            val valueEnd = valueStart + value.length
            val range = TextRange(valueStart, valueEnd)

            val fix = RewiewTwigAttributeNamingFix(match.range.first, match.range.last + 1)
            val descriptor = manager.createProblemDescriptor(
                file,
                range,
                "id/class values must be lowercase kebab-case",
                ProblemHighlightType.WEAK_WARNING,
                isOnTheFly,
                fix
            )
            problems.add(descriptor)
        }

        return if (problems.isEmpty()) emptyArray() else problems.toTypedArray()
    }

    private fun isKebabCase(token: String): Boolean {
        return token.matches(Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*"))
    }

    private class RewiewTwigAttributeNamingFix(
        private val startOffset: Int,
        private val endOffset: Int
    ) : LocalQuickFix {
        override fun getName(): String = "Convert id/class to kebab-case"

        override fun getFamilyName(): String = "Rewiew Twig formatting"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement?.containingFile ?: return
            val document = FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return
            val text = document.text

            if (startOffset < 0 || endOffset > text.length || startOffset >= endOffset) return
            val slice = text.substring(startOffset, endOffset)

            val fixed = TwigAutoFixer.fixAttributeNaming(slice)
            if (fixed == slice) return

            document.replaceString(startOffset, endOffset, fixed)
        }
    }
}
