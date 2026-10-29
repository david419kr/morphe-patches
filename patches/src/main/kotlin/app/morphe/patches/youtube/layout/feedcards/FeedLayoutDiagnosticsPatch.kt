package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

@Suppress("unused")
val feedLayoutDiagnosticsPatch = bytecodePatch(
    name = "Feed layout diagnostics",
    description = "Logs feed grid and item measurements to logcat under MorpheFeedDiag for troubleshooting.",
    default = false,
) {
    dependsOn(feedCardLayoutPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        val recycler = mutableClassDefBy("Landroid/support/v7/widget/RecyclerView;")
        val onViewAdded = recycler.methods.single {
            it.name == "onViewAdded" && it.parameterTypes == listOf("Landroid/view/View;")
        }
        onViewAdded.addInstruction(
            0,
            "invoke-static {p0}, Lapp/morphe/extension/youtube/patches/FeedLayoutDiagnostics;->onChildAdded(Landroid/support/v7/widget/RecyclerView;)V",
        )
    }
}
