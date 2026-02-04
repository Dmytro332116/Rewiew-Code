package com.rewiew.autofmt.util

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.rewiew.autofmt.settings.AutoFormatSettings

object AutoFormatRunner {
    private val IN_PROGRESS = Key.create<Boolean>("rewiew.autofmt.in_progress")
    private val JS_EXTENSIONS = listOf("js", "mjs", "cjs", "jsx")

    fun scheduleFormat(project: Project, psiFile: PsiFile) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            formatPsiFile(project, psiFile)
        }
    }

    fun formatVirtualFile(project: Project, file: VirtualFile, settings: AutoFormatSettings): Boolean {
        if (!TargetFileType.isSupported(project, file, settings)) return false
        if (!ProjectFileIndex.getInstance(project).isInContent(file)) return false
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false
        return formatPsiFile(project, psiFile)
    }

    fun formatPsiFile(project: Project, psiFile: PsiFile): Boolean {
        if (!psiFile.isValid || !psiFile.isWritable) return false
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return false
        if (document.getUserData(IN_PROGRESS) == true) return false

        document.putUserData(IN_PROGRESS, true)
        try {
            WriteCommandAction.runWriteCommandAction(project, "Rewiew Autoformat", null, Runnable {
                PsiDocumentManager.getInstance(project).commitDocument(document)
                ReformatCodeProcessor(project, psiFile, null, false).run()
            })
        } finally {
            document.putUserData(IN_PROGRESS, null)
        }

        return true
    }

    fun formatAll(project: Project, settings: AutoFormatSettings): Int {
        val scope = GlobalSearchScope.projectScope(project)
        val files = mutableListOf<VirtualFile>()

        if (settings.enableCss) {
            files.addAll(FilenameIndex.getAllFilesByExt(project, "css", scope))
        }
        if (settings.enableJs) {
            JS_EXTENSIONS.forEach { ext ->
                files.addAll(FilenameIndex.getAllFilesByExt(project, ext, scope))
            }
        }
        if (settings.enableTwig) {
            files.addAll(FilenameIndex.getAllFilesByExt(project, "twig", scope))
        }

        val unique = files.distinct()
        var count = 0
        val psiManager = PsiManager.getInstance(project)

        WriteCommandAction.runWriteCommandAction(project, "Rewiew Autoformat", null, Runnable {
            unique.forEach { vf ->
                val psiFile = psiManager.findFile(vf) ?: return@forEach
                if (formatPsiFile(project, psiFile)) count++
            }
        })

        return count
    }
}
