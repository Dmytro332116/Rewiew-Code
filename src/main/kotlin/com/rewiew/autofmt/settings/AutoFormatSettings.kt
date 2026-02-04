package com.rewiew.autofmt.settings

import com.intellij.util.xmlb.annotations.OptionTag

class AutoFormatSettings {
    @OptionTag("format_on_save")
    var formatOnSave: Boolean = true

    @OptionTag("format_on_commit")
    var formatOnCommit: Boolean = true

    @OptionTag("enable_css")
    var enableCss: Boolean = true

    @OptionTag("enable_js")
    var enableJs: Boolean = true

    @OptionTag("enable_twig")
    var enableTwig: Boolean = true

    @OptionTag("formatter_command")
    var formatterCommand: String = "tools/rewiew/format.sh"

    @OptionTag("inspector_command")
    var inspectorCommand: String = "tools/rewiew/inspect.sh"

    @OptionTag("inspection_profile_path")
    var inspectionProfilePath: String = ".idea/inspectionProfiles/Project_Default.xml"
}
