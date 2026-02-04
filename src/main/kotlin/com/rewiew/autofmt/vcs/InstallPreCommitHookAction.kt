package com.rewiew.autofmt.vcs

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class InstallPreCommitHookAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = AutoFormatSettingsService.getInstance(project).state

        if (settings.formatterCommand.isBlank() || settings.inspectorCommand.isBlank()) {
            notify(project, "Configure formatter and inspector commands in settings first.", NotificationType.WARNING)
            return
        }

        val hooksDir = resolveHooksDir(project) ?: run {
            notify(project, "Could not locate .git/hooks. Is this a Git repository?", NotificationType.ERROR)
            return
        }

        try {
            Files.createDirectories(hooksDir)
        } catch (ex: IOException) {
            notify(project, "Failed to create hooks directory: ${ex.message}", NotificationType.ERROR)
            return
        }

        val hookFile = hooksDir.resolve("pre-commit")
        val content = PreCommitHookTemplate.render(settings)

        if (Files.exists(hookFile)) {
            val existing = Files.readString(hookFile, StandardCharsets.UTF_8)
            if (!existing.contains(PreCommitHookTemplate.marker())) {
                val result = Messages.showYesNoDialog(
                    project,
                    "A pre-commit hook already exists. Overwrite it with Rewiew Autoformat hook?",
                    "Overwrite Pre-Commit Hook",
                    Messages.getWarningIcon()
                )
                if (result != Messages.YES) return
            }
        }

        try {
            Files.writeString(hookFile, content, StandardCharsets.UTF_8)
            hookFile.toFile().setExecutable(true)
        } catch (ex: IOException) {
            notify(project, "Failed to write pre-commit hook: ${ex.message}", NotificationType.ERROR)
            return
        }

        notify(project, "Pre-commit hook installed at ${hookFile}", NotificationType.INFORMATION)
    }

    private fun resolveHooksDir(project: Project): Path? {
        val basePath = project.basePath ?: return null
        val gitPath = Paths.get(basePath, ".git")
        val gitDir = when {
            Files.isDirectory(gitPath) -> gitPath
            Files.isRegularFile(gitPath) -> parseGitDirFile(gitPath, basePath)
            else -> null
        } ?: return null

        return gitDir.resolve("hooks")
    }

    private fun parseGitDirFile(gitFile: Path, basePath: String): Path? {
        return try {
            val content = Files.readString(gitFile, StandardCharsets.UTF_8).trim()
            val prefix = "gitdir:"
            if (!content.startsWith(prefix)) return null
            val path = content.removePrefix(prefix).trim()
            val resolved = Paths.get(path)
            if (resolved.isAbsolute) resolved else Paths.get(basePath).resolve(resolved).normalize()
        } catch (_: IOException) {
            null
        }
    }

    private fun notify(project: Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RewiewAutoformat")
            .createNotification(message, type)
            .notify(project)
    }
}
