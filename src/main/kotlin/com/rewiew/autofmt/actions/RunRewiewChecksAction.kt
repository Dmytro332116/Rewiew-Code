package com.rewiew.autofmt.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.rewiew.autofmt.settings.AutoFormatSettingsService
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

class RunRewiewChecksAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = AutoFormatSettingsService.getInstance(project).state

        val basePath = project.basePath ?: run {
            notify(project, "Project path not found.", NotificationType.ERROR)
            return
        }

        val inspectorCmd = settings.inspectorCommand.trim()
        if (inspectorCmd.isBlank()) {
            notify(project, "Configure Inspector command in settings.", NotificationType.WARNING)
            return
        }

        val profilePath = resolvePath(basePath, settings.inspectionProfilePath.ifBlank { ".idea/inspectionProfiles/Project_Default.xml" })
        val outputDir = Paths.get(basePath, ".idea", "rewiew-inspection-results")
        Files.createDirectories(outputDir)

        val inspectorPath = resolvePath(basePath, inspectorCmd)
        val cmd = mutableListOf<String>()
        if (inspectorPath.endsWith("inspect.sh") || inspectorPath.endsWith("inspect.bat")) {
            cmd.add(inspectorPath)
        } else {
            cmd.add(inspectorPath)
            cmd.add("inspect")
        }
        cmd.add(basePath)
        cmd.add(profilePath)
        cmd.add(outputDir.toString())
        cmd.add("-d")
        cmd.add(basePath)
        cmd.add("-format")
        cmd.add("xml")
        cmd.add("-v0")

        var report: String? = null
        val ok = ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                val process = ProcessBuilder(cmd)
                    .directory(File(basePath))
                    .redirectErrorStream(true)
                    .start()

                process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    report = buildReport(basePath, outputDir)
                }
            },
            "Running Rewiew Checks",
            true,
            project
        )

        if (!ok || report == null) {
            notify(project, "Inspection failed. Check PhpStorm path and profile.", NotificationType.ERROR)
            return
        }

        val reportFile = Paths.get(basePath, ".idea", "rewiew-report.txt")
        Files.writeString(reportFile, report!!, StandardCharsets.UTF_8)

        val vf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(reportFile)
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true)
        }

        notify(project, "Rewiew checks finished. Report saved to .idea/rewiew-report.txt", NotificationType.INFORMATION)
    }

    private fun resolvePath(basePath: String, path: String): String {
        val p = Paths.get(path)
        return if (p.isAbsolute) path else Paths.get(basePath).resolve(path).normalize().toString()
    }

    private fun buildReport(basePath: String, outputDir: Path): String {
        val xmlFiles = Files.list(outputDir).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".xml") }.toList()
        }

        val builder = StringBuilder()
        builder.append("Rewiew Checks Report\n")
        builder.append("Project: ").append(basePath).append("\n\n")

        if (xmlFiles.isEmpty()) {
            builder.append("No inspection XML files found.\n")
            return builder.toString()
        }

        val dbf = DocumentBuilderFactory.newInstance()
        val grouped = linkedMapOf(
            "Twig/HTML" to mutableListOf<String>(),
            "CSS" to mutableListOf<String>(),
            "JavaScript" to mutableListOf<String>()
        )

        xmlFiles.forEach { xml ->
            val doc = dbf.newDocumentBuilder().parse(xml.toFile())
            val problems = doc.getElementsByTagName("problem")
            for (i in 0 until problems.length) {
                val node = problems.item(i)
                val children = node.childNodes
                var file = ""
                var line = ""
                var description = ""
                var problemClass = ""
                var column = ""

                for (j in 0 until children.length) {
                    val c = children.item(j)
                    when (c.nodeName) {
                        "file" -> file = c.textContent
                        "line" -> line = c.textContent
                        "description" -> description = c.textContent
                        "problem_class" -> problemClass = c.textContent
                        "column" -> column = c.textContent
                    }
                }

                if (file.isBlank()) continue

                val normalized = file
                    .removePrefix("file://")
                    .replace("\$PROJECT_DIR\$", basePath)

                if (!isSupportedFile(normalized)) continue

                val rel = normalized.removePrefix(basePath).trimStart('/', '\\')
                val msg = description.ifBlank { problemClass.ifBlank { "Issue" } }
                val lineStr = if (line.isNotBlank()) ":$line" else ""
                val colStr = if (column.isNotBlank()) ":$column" else ""
                val suggestion = buildSuggestion(msg)

                val entry = buildString {
                    append(rel).append(lineStr).append(colStr).append(": ").append(msg)
                    if (suggestion.isNotBlank()) {
                        append("\n  -> ").append(suggestion)
                    }
                }

                when {
                    rel.lowercase().endsWith(".twig") -> grouped["Twig/HTML"]?.add(entry)
                    rel.lowercase().endsWith(".css") -> grouped["CSS"]?.add(entry)
                    rel.lowercase().endsWith(".js") || rel.lowercase().endsWith(".mjs") ||
                        rel.lowercase().endsWith(".cjs") || rel.lowercase().endsWith(".jsx") ->
                        grouped["JavaScript"]?.add(entry)
                }
            }
        }

        val allIssues = grouped.values.flatten()
        if (allIssues.isEmpty()) {
            builder.append("No issues found for CSS/JS/Twig.\n")
        } else {
            grouped.forEach { (title, list) ->
                if (list.isEmpty()) return@forEach
                builder.append(title).append("\n")
                list.forEach { builder.append(it).append("\n") }
                builder.append("\n")
            }
        }

        builder.append("\nNotes:\n")
        builder.append("- Auto-fix is available via Tools -> Auto-Fix CSS/JS/Twig\n")
        builder.append("- Some issues require manual fixes\n")

        return builder.toString()
    }

    private fun buildSuggestion(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("kebab") || lower.contains("id/class") ->
                "Use lowercase kebab-case (e.g. my-button)"
            lower.contains("space") && lower.contains("{{") ->
                "Add spaces inside {{ }}"
            lower.contains("unused") ->
                "Remove it or use it"
            lower.contains("semicolon") ->
                "Add missing semicolon"
            else -> ""
        }
    }

    private fun isSupportedFile(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".mjs") ||
            lower.endsWith(".cjs") || lower.endsWith(".jsx") || lower.endsWith(".twig")
    }

    private fun notify(project: com.intellij.openapi.project.Project, message: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("RewiewAutoformat")
            .createNotification(message, type)
            .notify(project)
    }
}
