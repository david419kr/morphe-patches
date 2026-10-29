package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.misc.litho.filter.lithoFilterPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

// Keep the existing selection compatible while retiring the unsuccessful width overrides.
@Suppress("unused")
val feedGridFixPatch = bytecodePatch(
    name = "Fix tablet home feed layout (experimental)",
    description = "Uses the upstream list-section lifecycle fix. No forced widths or template overrides.",
    default = false,
) {
    dependsOn(lithoFilterPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)
}
