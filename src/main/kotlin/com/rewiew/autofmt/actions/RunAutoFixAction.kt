package com.rewiew.autofmt.actions

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.TargetFileType
import com.rewiew.autofmt.util.TwigAutoFixer

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
        WriteCommandAction.runWriteCommandAction(project, "Rewiew Auto-Fix", null, Runnable {
            targetFiles.forEach { vf ->
                val psiFile = psiManager.findFile(vf) ?: return@forEach
                if (!psiFile.isValid || !psiFile.isWritable) return@forEach
                PsiDocumentManager.getInstance(project).commitAllDocuments()

                // First, run standard formatter
                ReformatCodeProcessor(project, psiFile, null, false).run()

                // Then, apply lightweight Twig auto-fixes
                if (vf.extension?.lowercase() == "twig") {
                    val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@forEach
                    val updated = TwigAutoFixer.applyAll(doc.text)
                    if (updated != doc.text) {
                        doc.setText(updated)
                    }
                }
            }
        })

        notify(project, "Auto-fix completed for ${targetFiles.size} file(s).", NotificationType.INFORMATION)
    }

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RewiewAutoformat")
            .createNotification(message, type)
            .notify(project)
    }
}
