package com.rewiew.autofmt.actions

import com.intellij.codeInsight.actions.CodeCleanupCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.CssAutoFixer
import com.rewiew.autofmt.util.TargetFileType
import com.rewiew.autofmt.util.TwigAutoFixer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class RunAutoFixAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = AutoFormatSettingsService.getInstance(project).state

        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
            ?: e.getData(CommonDataKeys.VIRTUAL_FILE)?.let { listOf(it) }
            ?: emptyList()

        if (files.isEmpty()) {
            notify(project, "No files selected.", NotificationType.WARNING)
            return
        }

        val targetFiles = files.filter { TargetFileType.isSupported(project, it, settings) }
        if (targetFiles.isEmpty()) {
            notify(project, "No supported files selected (CSS/JS/Twig).", NotificationType.WARNING)
            return
        }

        val psiManager = PsiManager.getInstance(project)
        val psiFiles = targetFiles.mapNotNull { vf ->
            psiManager.findFile(vf)?.takeIf { it.isValid && it.isWritable }
        }

        if (psiFiles.isEmpty()) {
            notify(project, "No writable files to fix.", NotificationType.WARNING)
            return
        }

        val beforeMap = psiFiles.associateWith { psiFile ->
            PsiDocumentManager.getInstance(project).getDocument(psiFile)?.text ?: ""
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                // CSS safe fixes first (helps formatter/cleanup handle syntax errors)
                WriteCommandAction.runWriteCommandAction(project, "Rewiew CSS Auto-Fix", null, Runnable {
                    psiFiles.forEach { psiFile ->
                        val vf = psiFile.virtualFile ?: return@forEach
                        if (vf.extension?.lowercase() != "css") return@forEach
                        val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@forEach
                        val (updated, _) = CssAutoFixer.applyAll(doc.text)
                        if (updated != doc.text) doc.setText(updated)
                    }
                })

                // Apply IDE cleanup/quick-fixes where available
                CodeCleanupCodeProcessor(project, psiFiles.toTypedArray(), null, false).run()

                // Reformat after cleanup
                WriteCommandAction.runWriteCommandAction(project, "Rewiew Auto-Fix", null, Runnable {
                    psiFiles.forEach { psiFile ->
                        ReformatCodeProcessor(project, psiFile, null, false).run()
                    }
                })

                // Safe Twig auto-fixes
                WriteCommandAction.runWriteCommandAction(project, "Rewiew Twig Auto-Fix", null, Runnable {
                    psiFiles.forEach { psiFile ->
                        val vf = psiFile.virtualFile ?: return@forEach
                        if (vf.extension?.lowercase() != "twig") return@forEach
                        val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@forEach
                        val updated = TwigAutoFixer.applyAll(doc.text)
                        if (updated != doc.text) {
                            doc.setText(updated)
                        }
                    }
                })
            },
            "Running Auto-Fix",
            true,
            project
        )

        val report = buildAutoFixReport(project, beforeMap)
        val reportFile = Paths.get(project.basePath ?: ".", ".idea", "rewiew-autofix.txt")
        Files.createDirectories(reportFile.parent)
        Files.writeString(reportFile, report, StandardCharsets.UTF_8)

        val vf = com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByNioFile(reportFile)
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true)
        }

        notify(project, "Auto-fix completed. Report saved to .idea/rewiew-autofix.txt", NotificationType.INFORMATION)
    }

    private fun buildAutoFixReport(project: com.intellij.openapi.project.Project, beforeMap: Map<com.intellij.psi.PsiFile, String>): String {
        val basePath = project.basePath ?: ""
        val sb = StringBuilder()
        sb.append("Rewiew Auto-Fix Report\n\n")

        beforeMap.forEach { (psiFile, beforeText) ->
            val vf = psiFile.virtualFile ?: return@forEach
            val afterText = PsiDocumentManager.getInstance(project).getDocument(psiFile)?.text ?: ""
            if (beforeText == afterText) return@forEach

            val rel = vf.path.removePrefix(basePath).trimStart('/', '\\')
            val diff = countAddedPunctuation(beforeText, afterText)
            sb.append(rel).append(":\n")
            sb.append("  Added : ").append(diff.addedColons).append("\n")
            sb.append("  Added ;: ").append(diff.addedSemicolons).append("\n")
            sb.append("  Added (): ").append(diff.addedParens).append("\n")
            sb.append("  Added {}: ").append(diff.addedBraces).append("\n")
            sb.append("  Added []: ").append(diff.addedBrackets).append("\n\n")
        }

        if (sb.lines().count { it.endsWith(":") } == 0) {
            sb.append("No changes were applied.\n")
        }

        sb.append("\nNotes:\n")
        sb.append("- Auto-fix applies IDE cleanup + formatter + safe Twig fixes\n")
        sb.append("- Some logical issues still require manual changes\n")

        return sb.toString()
    }

    private fun countAddedPunctuation(beforeText: String, afterText: String): PunctuationDiff {
        fun count(text: String, ch: Char): Int = text.count { it == ch }

        val beforeColons = count(beforeText, ':')
        val afterColons = count(afterText, ':')

        val beforeSemis = count(beforeText, ';')
        val afterSemis = count(afterText, ';')

        val beforeParens = count(beforeText, '(') + count(beforeText, ')')
        val afterParens = count(afterText, '(') + count(afterText, ')')

        val beforeBraces = count(beforeText, '{') + count(beforeText, '}')
        val afterBraces = count(afterText, '{') + count(afterText, '}')

        val beforeBrackets = count(beforeText, '[') + count(beforeText, ']')
        val afterBrackets = count(afterText, '[') + count(afterText, ']')

        return PunctuationDiff(
            addedColons = (afterColons - beforeColons).coerceAtLeast(0),
            addedSemicolons = (afterSemis - beforeSemis).coerceAtLeast(0),
            addedParens = (afterParens - beforeParens).coerceAtLeast(0),
            addedBraces = (afterBraces - beforeBraces).coerceAtLeast(0),
            addedBrackets = (afterBrackets - beforeBrackets).coerceAtLeast(0)
        )
    }

    private data class PunctuationDiff(
        val addedColons: Int,
        val addedSemicolons: Int,
        val addedParens: Int,
        val addedBraces: Int,
        val addedBrackets: Int
    )

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RewiewAutoformat")
            .createNotification(message, type)
            .notify(project)
    }
}
