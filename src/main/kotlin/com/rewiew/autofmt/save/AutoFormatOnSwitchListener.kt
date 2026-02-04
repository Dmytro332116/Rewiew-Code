package com.rewiew.autofmt.save

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.AutoFormatRunner

class AutoFormatOnSwitchListener(private val project: Project) : FileEditorManagerListener {
    override fun selectionChanged(event: FileEditorManagerEvent) {
        val settings = AutoFormatSettingsService.getInstance(project).state
        if (!settings.formatOnSave) return

        val oldFile = event.oldFile ?: return
        val doc = FileDocumentManager.getInstance().getDocument(oldFile) ?: return
        if (!FileDocumentManager.getInstance().isDocumentUnsaved(doc)) return

        AutoFormatRunner.formatVirtualFile(project, oldFile, settings)
    }
}
