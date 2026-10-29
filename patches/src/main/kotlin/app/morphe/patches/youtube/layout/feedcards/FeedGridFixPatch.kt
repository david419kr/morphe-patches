package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private object FeedRenderInfoFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("I"),
    filters = listOf(string("enable_imp_for_single_column_grid")),
)

@Suppress("unused")
val feedGridFixPatch = bytecodePatch(
    name = "Fix tablet home feed layout (experimental)",
    description = "Corrects the full-width decision for known video and shelf templates on large home feeds.",
    default = false,
) {
    dependsOn(sharedExtensionPatch, navigationBarHookPatch, fixProtoLibraryPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        val element = ElementItemConstructorFingerprint
        val elementType = element.originalClassDef.type
        val bufferField = element.instructionMatches[1].getFieldAccessed()
        element.classDef.apply {
            interfaces.add("Lapp/morphe/extension/youtube/patches/FeedGridFix\$ElementBytes;")
            methods.add(
                ImmutableMethod(
                    type, "patch_getFeedGridElementBytes", listOf(), "[B",
                    AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
                    MutableMethodImplementation(2),
                ).toMutable().apply {
                    addInstructions(0, "iget-object v0, p0, $bufferField\nreturn-object v0")
                }
            )
        }

        val method = FeedRenderInfoFingerprint.method
        val instructions = method.implementation!!.instructions
        val matches = instructions.indices.filter { index ->
            if (index + 1 >= instructions.size || instructions[index].opcode != Opcode.INVOKE_VIRTUAL
                || instructions[index + 1].opcode != Opcode.INVOKE_VIRTUAL) return@filter false
            val setter = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
            val next = (instructions[index + 1] as? ReferenceInstruction)?.reference as? MethodReference
            setter?.parameterTypes?.map(CharSequence::toString) == listOf("Z") && setter.returnType == "V"
                    && next?.definingClass == elementType && next.parameterTypes.isEmpty()
        }
        check(matches.size == 1) { "Could not uniquely locate feed full-span setter" }
        val index = matches.single()
        val setter = instructions[index] as FiveRegisterInstruction
        val next = instructions[index + 1] as FiveRegisterInstruction
        val setterReference = (instructions[index] as ReferenceInstruction).reference as MethodReference
        val builderRegister = setter.registerC
        val spanRegister = setter.registerD
        val elementRegister = next.registerC
        check(builderRegister <= 15 && spanRegister <= 15 && elementRegister <= 15) { "Unsupported feed registers" }
        method.addInstructions(
            index + 1,
            """
                invoke-static {v$elementRegister, v$spanRegister}, Lapp/morphe/extension/youtube/patches/FeedGridFix;->fullSpan(Ljava/lang/Object;Z)Z
                move-result v$spanRegister
                invoke-virtual {v$builderRegister, v$spanRegister}, $setterReference
            """
        )
    }
}
