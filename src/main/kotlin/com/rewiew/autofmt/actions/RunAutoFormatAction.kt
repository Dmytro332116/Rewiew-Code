package com.rewiew.autofmt.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.AutoFormatRunner
import com.rewiew.autofmt.util.TargetFileType

class RunAutoFormatAction : DumbAwareAction() {
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

        var count = 0
        targetFiles.forEach { vf ->
            if (AutoFormatRunner.formatVirtualFile(project, vf, settings)) count++
        }

        notify(project, "Formatted $count file(s).", NotificationType.INFORMATION)
    }

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RewiewAutoformat")
            .createNotification(message, type)
            .notify(project)
    }
}
