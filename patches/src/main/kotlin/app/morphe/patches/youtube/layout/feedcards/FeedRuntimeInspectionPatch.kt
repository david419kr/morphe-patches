package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.all.misc.debugging.enableAndroidDebuggingPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

@Suppress("unused")
val feedRuntimeInspectionPatch = resourcePatch(
    name = "Enable feed runtime inspection",
    description = "Enables Android debugger attachment for live feed object inspection. May slow down the app; omit from normal builds.",
    default = false,
) {
    dependsOn(enableAndroidDebuggingPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)
}
