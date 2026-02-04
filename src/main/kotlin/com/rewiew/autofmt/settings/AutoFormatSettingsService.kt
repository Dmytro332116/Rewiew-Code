package com.rewiew.autofmt.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "RewiewAutoFormatSettings",
    storages = [Storage("autoformat.xml")]
)
class AutoFormatSettingsService(private val project: Project) : PersistentStateComponent<AutoFormatSettings> {
    private var state = AutoFormatSettings()

    override fun getState(): AutoFormatSettings = state

    override fun loadState(state: AutoFormatSettings) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    companion object {
        fun getInstance(project: Project): AutoFormatSettingsService =
            project.getService(AutoFormatSettingsService::class.java)
    }
}
