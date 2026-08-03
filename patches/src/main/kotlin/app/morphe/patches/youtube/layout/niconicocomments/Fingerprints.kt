package app.morphe.patches.youtube.layout.niconicocomments

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
import app.morphe.patches.youtube.shared.LayoutConstructorFingerprint

/**
 * Finds YouTube's full-size player overlay without depending on SponsorBlock.
 */
internal object NiconicoCommentsOverlayFingerprint : Fingerprint(
    classFingerprint = LayoutConstructorFingerprint,
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        resourceLiteral(ResourceType.ID, "inset_overlay_view_layout"),
        checkCast("Landroid/widget/FrameLayout;", MatchAfterWithin(20)),
    ),
)
