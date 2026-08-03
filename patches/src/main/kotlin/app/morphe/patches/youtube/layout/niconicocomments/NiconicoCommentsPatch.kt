package app.morphe.patches.youtube.layout.niconicocomments

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CONTROLLER_CLASS =
    "Lapp/morphe/extension/youtube/patches/niconicocomments/NiconicoCommentsController;"

@Suppress("unused")
val niconicoCommentsPatch = bytecodePatch(
    name = "Niconico-style comments (PoC)",
    description = "Adds a playback-synchronized proof-of-concept scrolling comment overlay.",
) {
    dependsOn(
        sharedExtensionPatch,
        videoIdPatch,
        videoInformationPatch,
        playerTypeHookPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        hookVideoId(
            "$EXTENSION_CONTROLLER_CLASS->onVideoIdChanged(Ljava/lang/String;)V",
        )

        NiconicoCommentsOverlayFingerprint.let {
            it.method.apply {
                val checkCastIndex = it.instructionMatches.last().index
                val frameLayoutRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$frameLayoutRegister}, " +
                        "$EXTENSION_CONTROLLER_CLASS->initialize(Landroid/view/ViewGroup;)V",
                )
            }
        }
    }
}
