package io.github.kimihrmn.clicolorhints

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.InlayHintsPassFactoryInternal
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows

/**
 * Per-user (application-level) settings, stored in the IDE config dir (`cliColorHints.xml`),
 * never inside a project.
 */
@Service(Service.Level.APP)
@State(name = "CliColorHints", storages = [Storage("cliColorHints.xml", roamingType = RoamingType.DEFAULT)])
class ColorHintsSettings : PersistentStateComponent<ColorHintsSettings.State> {

    class State {
        /** Fully qualified names of annotations that mark color-coded strings. */
        var annotationFqns: MutableList<String> = mutableListOf(DEFAULT_ANNOTATION_FQN)
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    /** Normalized FQNs (trimmed, non-blank, distinct). */
    var annotationFqns: List<String>
        get() = state.annotationFqns.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        set(value) {
            state.annotationFqns = value.map { it.trim() }.filter { it.isNotEmpty() }.distinct().toMutableList()
        }

    companion object {
        const val DEFAULT_ANNOTATION_FQN: String = "com.example.shop.ColorCoded"

        fun getInstance(): ColorHintsSettings =
            ApplicationManager.getApplication().getService(ColorHintsSettings::class.java)

        /** Re-run highlighting and inlays in all open projects (after the FQNs changed). */
        fun refreshOpenProjects() {
            InlayHintsPassFactoryInternal.forceHintsUpdateOnNextPass()
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) DaemonCodeAnalyzer.getInstance(project).restart("CLI color hints: annotation FQNs changed")
            }
        }
    }
}

/** Settings → Tools → CLI Color Hints. */
class ColorHintsConfigurable : BoundSearchableConfigurable("CLI Color Hints", "io.github.kimihrmn.clicolorhints.settings") {

    override fun createPanel() = panel {
        val settings = ColorHintsSettings.getInstance()
        row {
            label("Annotations that mark color-coded strings (fully qualified name, one per line):")
        }
        row {
            textArea()
                .rows(6)
                .align(AlignX.FILL)
                .bindText(
                    { settings.annotationFqns.joinToString("\n") },
                    { settings.annotationFqns = it.lines() },
                )
                .comment(
                    "Strings passed to parameters, or assigned to / returned from declarations, annotated with any of " +
                        "these get completion, coloring, swatches and the preview. Stored per user, not in the project.",
                )
        }
    }

    override fun apply() {
        super.apply()
        ColorHintsSettings.refreshOpenProjects()
    }
}
