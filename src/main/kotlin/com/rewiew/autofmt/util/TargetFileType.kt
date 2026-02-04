package com.rewiew.autofmt.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.rewiew.autofmt.settings.AutoFormatSettings

object TargetFileType {
    private val jsExtensions = setOf("js", "mjs", "cjs", "jsx")

    fun isSupported(project: Project, file: VirtualFile, settings: AutoFormatSettings): Boolean {
        if (file.isDirectory) return false
        if (!ProjectFileIndex.getInstance(project).isInContent(file)) return false

        val ext = file.extension?.lowercase() ?: return false
        return when {
            settings.enableCss && ext == "css" -> true
            settings.enableJs && jsExtensions.contains(ext) -> true
            settings.enableTwig && ext == "twig" -> true
            else -> false
        }
    }
}
