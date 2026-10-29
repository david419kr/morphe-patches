package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

@Suppress("unused")
val feedLayoutDiagnosticsPatch = bytecodePatch(
    name = "Feed layout diagnostics",
    description = "Logs feed grid and item measurements to logcat under MorpheFeedDiag for troubleshooting.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        val recycler = mutableClassDefBy("Landroid/support/v7/widget/RecyclerView;")
        val onViewAdded = recycler.methods.firstOrNull {
            it.name == "onViewAdded" && it.parameterTypes == listOf("Landroid/view/View;")
        }
        val call = "invoke-static {p0}, Lapp/morphe/extension/youtube/patches/FeedLayoutDiagnostics;->onChildAdded(Landroid/support/v7/widget/RecyclerView;)V"
        if (onViewAdded != null) {
            onViewAdded.addInstruction(0, call)
        } else {
            recycler.methods.add(
                ImmutableMethod(
                    recycler.type, "onViewAdded",
                    listOf(ImmutableMethodParameter("Landroid/view/View;", null, null)),
                    "V", AccessFlags.PUBLIC.value, null, null, MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(0, "invoke-super {p0, p1}, Landroid/view/ViewGroup;->onViewAdded(Landroid/view/View;)V\n$call\nreturn-void")
                }
            )
        }
    }
}
