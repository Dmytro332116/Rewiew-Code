package com.rewiew.autofmt.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JButton
import com.rewiew.autofmt.util.AutoFormatRunner

class AutoFormatConfigurable(private val project: Project) : SearchableConfigurable {
    private val settingsService = AutoFormatSettingsService.getInstance(project)

    private var formatOnSave = true
    private var formatOnCommit = true
    private var enableCss = true
    private var enableJs = true
    private var enableTwig = true
    private var formatterCommand = ""
    private var inspectorCommand = ""
    private var inspectionProfilePath = ""

    private var component: JComponent? = null

    override fun getId(): String = "com.rewiew.autofmt.settings"

    override fun getDisplayName(): String = "Rewiew Code Formatter"

    override fun createComponent(): JComponent {
        val ui = panel {
            group("On Save") {
                row {
                    checkBox("Format on save")
                        .bindSelected(::formatOnSave)
                }
            }
            group("Quick Actions") {
                row {
                    val btn = JButton("Format All CSS/JS/Twig in Project")
                    btn.addActionListener {
                        val settings = settingsService.state
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            {
                                AutoFormatRunner.formatAll(project, settings)
                            },
                            "Formatting All Files",
                            true,
                            project
                        )
                    }
                    cell(btn)
                }
            }
            group("On Commit") {
                row {
                    checkBox("Format before commit (IDE)")
                        .bindSelected(::formatOnCommit)
                }
            }
            group("File Types") {
                row {
                    checkBox("CSS").bindSelected(::enableCss)
                    checkBox("JavaScript").bindSelected(::enableJs)
                    checkBox("Twig").bindSelected(::enableTwig)
                }
            }
            group("Pre-Commit Hook (CLI)") {
                row("Formatter command") {
                    textField()
                        .bindText(::formatterCommand)
                        .comment("Default: tools/rewiew/format.sh (uses PHPSTORM_BIN or PHPSTORM_HOME)")
                }
                row("Inspector command") {
                    textField()
                        .bindText(::inspectorCommand)
                        .comment("Default: tools/rewiew/inspect.sh (uses PHPSTORM_BIN or PHPSTORM_HOME)")
                }
                row("Inspection profile") {
                    textField()
                        .bindText(::inspectionProfilePath)
                        .comment("Default: .idea/inspectionProfiles/Project_Default.xml")
                }
            }
            group("Notes") {
                row {
                    label("Pre-commit hook uses IDE command-line formatter/inspector and will not run if the IDE is open.")
                }
            }
        }
        component = ui
        reset()
        return ui
    }

    override fun isModified(): Boolean {
        val s = settingsService.state
        return formatOnSave != s.formatOnSave ||
            formatOnCommit != s.formatOnCommit ||
            enableCss != s.enableCss ||
            enableJs != s.enableJs ||
            enableTwig != s.enableTwig ||
            formatterCommand != s.formatterCommand ||
            inspectorCommand != s.inspectorCommand ||
            inspectionProfilePath != s.inspectionProfilePath
    }

    override fun apply() {
        val s = settingsService.state
        s.formatOnSave = formatOnSave
        s.formatOnCommit = formatOnCommit
        s.enableCss = enableCss
        s.enableJs = enableJs
        s.enableTwig = enableTwig
        s.formatterCommand = formatterCommand
        s.inspectorCommand = inspectorCommand
        s.inspectionProfilePath = inspectionProfilePath
    }

    override fun reset() {
        val s = settingsService.state
        formatOnSave = s.formatOnSave
        formatOnCommit = s.formatOnCommit
        enableCss = s.enableCss
        enableJs = s.enableJs
        enableTwig = s.enableTwig
        formatterCommand = s.formatterCommand
        inspectorCommand = s.inspectorCommand
        inspectionProfilePath = s.inspectionProfilePath
        component?.revalidate()
        component?.repaint()
    }

    override fun disposeUIResources() {
        component = null
    }
}
