package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.Compatibility
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
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private object FeedRenderInfoFingerprint : Fingerprint(
    returnType = "L",
    parameters = listOf("I"),
    filters = listOf(string("enable_imp_for_single_column_grid")),
)

private object FeedBindFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "I"),
    filters = listOf(string("LithoRecyclerViewSectionListControllerBinder#onBindViewHolder")),
)

@Suppress("unused")
val feedGridFixPatch = bytecodePatch(
    name = "Fix tablet home feed layout (experimental)",
    description = "Corrects the full-width decision for known video and shelf templates on large home feeds.",
    default = false,
) {
    dependsOn(sharedExtensionPatch, navigationBarHookPatch, fixProtoLibraryPatch)
    compatibleWith(Compatibility(
        packageName = "com.google.android.youtube",
        name = "YouTube",
        apkFileType = COMPATIBILITY_YOUTUBE.apkFileType,
        appIconColor = COMPATIBILITY_YOUTUBE.appIconColor,
        signatures = COMPATIBILITY_YOUTUBE.signatures,
        targets = COMPATIBILITY_YOUTUBE.targets.filter { it.version in setOf("21.20.405", "21.23.492") },
    ))

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

        val bind = FeedBindFingerprint.method
        val bindInstructions = bind.implementation!!.instructions
        val widthCalls = bindInstructions.indices.filter { i ->
            if (i < 1 || i + 5 >= bindInstructions.size
                || bindInstructions[i - 1].opcode != Opcode.INVOKE_VIRTUAL
                || bindInstructions[i].opcode != Opcode.INVOKE_VIRTUAL
                || bindInstructions[i + 1].opcode != Opcode.MOVE_RESULT
                || bindInstructions[i + 2].opcode != Opcode.INVOKE_VIRTUAL
                || bindInstructions[i + 3].opcode != Opcode.MOVE_RESULT
                || bindInstructions[i + 4].opcode != Opcode.INVOKE_VIRTUAL
                || bindInstructions[i + 5].opcode != Opcode.MOVE_RESULT) return@filter false
            val first = (bindInstructions[i] as? ReferenceInstruction)?.reference as? MethodReference
            val second = (bindInstructions[i + 2] as? ReferenceInstruction)?.reference as? MethodReference
            val check = (bindInstructions[i + 4] as? ReferenceInstruction)?.reference as? MethodReference
            first != null && second != null && check != null && first.definingClass == second.definingClass
                    && first.returnType == "I" && second.returnType == "I"
                    && first.parameterTypes.size == 1 && first.parameterTypes == second.parameterTypes
                    && check.returnType == "Z" && check.parameterTypes.map(CharSequence::toString) == listOf("I", "I")
        }
        check(widthCalls.size == 1) { "Could not uniquely locate feed width measurement" }
        val widthIndex = widthCalls.single()
        val fullCalls = bindInstructions.indices.filter { i ->
            if (i <= widthIndex || i + 2 >= bindInstructions.size
                || bindInstructions[i].opcode != Opcode.INVOKE_INTERFACE
                || bindInstructions[i + 1].opcode != Opcode.NEW_INSTANCE
                || bindInstructions[i + 2].opcode != Opcode.INVOKE_DIRECT) return@filter false
            val flag = (bindInstructions[i] as? ReferenceInstruction)?.reference as? MethodReference
            val constructor = (bindInstructions[i + 2] as? ReferenceInstruction)?.reference as? MethodReference
            flag != null && flag.returnType == "Z" && flag.parameterTypes.isEmpty()
                    && constructor?.name == "<init>" && constructor.parameterTypes.map(CharSequence::toString) == listOf("I", "I", "I", "I")
        }
        check(fullCalls.size == 1) { "Could not uniquely locate feed full-span getter" }
        val fullCall = bindInstructions[fullCalls.single()] as FiveRegisterInstruction
        val fullReference = (bindInstructions[fullCalls.single()] as ReferenceInstruction).reference as MethodReference
        val precedingCall = (bindInstructions[widthIndex - 1] as ReferenceInstruction).reference as MethodReference
        check(precedingCall.returnType == "V"
                && precedingCall.parameterTypes.map(CharSequence::toString) == listOf("Ljava/util/List;")) {
            "Unsupported feed item view call"
        }
        val binderRegister = (bindInstructions[widthIndex] as FiveRegisterInstruction).registerC
        val viewRegister = (bindInstructions[widthIndex - 1] as FiveRegisterInstruction).registerC
        val widthRegister = (bindInstructions[widthIndex + 1] as OneRegisterInstruction).registerA
        val scratchRegister = (bindInstructions[widthIndex + 5] as OneRegisterInstruction).registerA
        val modelRegister = fullCall.registerC
        check(listOf(binderRegister, viewRegister, widthRegister, scratchRegister, modelRegister).all { it <= 15 }
                && scratchRegister !in listOf(binderRegister, viewRegister, widthRegister, modelRegister)) {
            "Unsupported feed bind registers"
        }
        bind.addInstructions(
            widthIndex + 2,
            """
                invoke-interface {v$modelRegister}, $fullReference
                move-result v$scratchRegister
                invoke-static {v$binderRegister, v$viewRegister, v$widthRegister, v$scratchRegister}, Lapp/morphe/extension/youtube/patches/FeedGridFix;->adjustMeasuredWidth(Ljava/lang/Object;Landroid/view/View;IZ)I
                move-result v$widthRegister
            """
        )
    }
}
