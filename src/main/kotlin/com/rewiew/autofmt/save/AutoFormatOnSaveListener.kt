package com.rewiew.autofmt.save

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectLocator
import com.intellij.psi.PsiDocumentManager
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.AutoFormatRunner
import com.rewiew.autofmt.util.TargetFileType

class AutoFormatOnSaveListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        if (project.isDisposed) return

        val settings = AutoFormatSettingsService.getInstance(project).state
        if (!settings.formatOnSave) return
        if (!TargetFileType.isSupported(project, file, settings)) return

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.commitDocument(document)
        val psiFile = psiDocumentManager.getPsiFile(document) ?: return
        if (!psiFile.isValid || !psiFile.isWritable) return

        AutoFormatRunner.scheduleFormat(project, psiFile)
    }
}
