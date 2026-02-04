package com.rewiew.autofmt.vcs

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import com.rewiew.autofmt.util.TargetFileType
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class AutoFormatCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {
    private val project: Project = panel.project

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
        val settings = AutoFormatSettingsService.getInstance(project).state
        val formatCheckBox = JCheckBox("Format CSS/JS/Twig before commit", settings.formatOnCommit)
        val panelUi = JPanel(BorderLayout()).apply {
            val inner = JPanel().apply {
                add(formatCheckBox)
            }
            add(inner, BorderLayout.WEST)
        }

        return object : RefreshableOnComponent {
            override fun getComponent(): JComponent = panelUi

            override fun refresh() {
                formatCheckBox.isSelected = settings.formatOnCommit
            }

            override fun saveState() {
                settings.formatOnCommit = formatCheckBox.isSelected
            }

            override fun restoreState() {
                formatCheckBox.isSelected = settings.formatOnCommit
            }
        }
    }

    override fun beforeCheckin(): ReturnResult {
        val settings = AutoFormatSettingsService.getInstance(project).state
        val files = panel.virtualFiles.filter { TargetFileType.isSupported(project, it, settings) }
        if (files.isEmpty()) return ReturnResult.COMMIT

        if (settings.formatOnCommit) {
            reformatFiles(files)
        }

        return ReturnResult.COMMIT
    }

    private fun reformatFiles(files: List<VirtualFile>) {
        val psiManager = PsiManager.getInstance(project)
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                WriteCommandAction.runWriteCommandAction(project, "Rewiew Autoformat", null, Runnable {
                    files.forEach { file ->
                        val psiFile = psiManager.findFile(file) ?: return@forEach
                        ReformatCodeProcessor(project, psiFile, null, false).run()
                    }
                })
            },
            "Reformatting Files",
            false,
            project
        )
    }

    // Inspections on commit are handled by the pre-commit hook via inspect.sh.
}
