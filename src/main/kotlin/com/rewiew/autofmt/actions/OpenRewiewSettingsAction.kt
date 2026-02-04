package com.rewiew.autofmt.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.rewiew.autofmt.settings.AutoFormatConfigurable

class OpenRewiewSettingsAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AutoFormatConfigurable::class.java)
    }
}
