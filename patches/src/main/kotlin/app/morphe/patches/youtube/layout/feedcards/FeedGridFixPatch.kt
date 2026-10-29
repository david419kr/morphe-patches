package app.morphe.patches.youtube.layout.feedcards

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private object RecoveryBindFingerprint : Fingerprint(
    returnType = "V", parameters = listOf("L", "I"),
    filters = listOf(string("LithoRecyclerViewSectionListControllerBinder#onBindViewHolder")),
)

@Suppress("unused")
val feedGridFixPatch = bytecodePatch(
    name = "Fix tablet home feed layout (experimental)",
    description = "Detects stale feed view bindings against current item metadata and rebinds the existing dataset. Does not force widths.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith(Compatibility(
        packageName = "com.google.android.youtube", name = "YouTube",
        apkFileType = COMPATIBILITY_YOUTUBE.apkFileType,
        appIconColor = COMPATIBILITY_YOUTUBE.appIconColor,
        signatures = COMPATIBILITY_YOUTUBE.signatures,
        targets = COMPATIBILITY_YOUTUBE.targets.filter { it.version in setOf("21.20.405", "21.23.492") },
    ))
    execute {
        val bind = RecoveryBindFingerprint.method
        val ins = bind.implementation!!.instructions
        fun ref(i: Int) = (ins[i] as? ReferenceInstruction)?.reference as? MethodReference
        val widths = ins.indices.filter { i ->
            i + 5 < ins.size && ins[i].opcode == Opcode.INVOKE_VIRTUAL
                    && ins[i+1].opcode == Opcode.MOVE_RESULT && ins[i+2].opcode == Opcode.INVOKE_VIRTUAL
                    && ins[i+3].opcode == Opcode.MOVE_RESULT && ins[i+4].opcode == Opcode.INVOKE_VIRTUAL
                    && ref(i)?.returnType == "I" && ref(i+2)?.returnType == "I"
                    && ref(i)?.definingClass == ref(i+2)?.definingClass
                    && ref(i)?.parameterTypes?.size == 1
                    && ref(i)?.parameterTypes == ref(i+2)?.parameterTypes
                    && ref(i+4)?.returnType == "Z" && ref(i+4)?.parameterTypes?.map { it.toString() } == listOf("I", "I")
        }
        check(widths.size == 1) { "Cannot uniquely locate feed width getter" }
        val widthRef = ref(widths.single())!!
        val binder = mutableClassDefBy(widthRef.definingClass)
        val itemType = widthRef.parameterTypes.single().toString()
        val fullIndices = ins.indices.filter { i -> i+2 < ins.size
                && ins[i].opcode == Opcode.INVOKE_INTERFACE && ins[i+1].opcode == Opcode.NEW_INSTANCE
                && ref(i)?.returnType == "Z" && ref(i)?.parameterTypes?.isEmpty() == true
                && ref(i+2)?.name == "<init>" && ref(i+2)?.parameterTypes?.map { it.toString() } == listOf("I","I","I","I") }
        check(fullIndices.size == 1) { "Cannot uniquely locate render full-span getter" }
        val fullRef = ref(fullIndices.single())!!
        val paramsType = ref(fullIndices.single()+2)!!.definingClass
        val itemGetter = ins.mapNotNull { (it as? ReferenceInstruction)?.reference as? MethodReference }
            .filter { it.definingClass == itemType && it.returnType == fullRef.definingClass && it.parameterTypes.isEmpty() }.distinct().single()
        val itemsField = ins.mapNotNull { (it as? ReferenceInstruction)?.reference as? FieldReference }
            .filter { it.definingClass == binder.type && it.type == "Ljava/util/List;" }.distinct().first()
        val listField = binder.fields.single { it.type == "Landroid/support/v7/widget/RecyclerView;" }
        val adapterType = RecoveryBindFingerprint.originalClassDef.superclass!!
        val adapterField = binder.fields.single { it.type == adapterType }
        val notify = classDefBy(adapterType).methods.filter { m ->
            m.name != "<init>" && m.returnType == "V" && m.parameterTypes.isEmpty()
                    && m.implementation?.instructions?.toList()?.let { code -> code.size == 3
                    && code[0].opcode == Opcode.IGET_OBJECT && code[1].opcode == Opcode.INVOKE_VIRTUAL
                    && code[2].opcode == Opcode.RETURN_VOID } == true
        }.single()
        val params = mutableClassDefBy(paramsType)
        val posGetter = classDefBy(params.superclass!!).methods.single { it.returnType == "I" && it.parameterTypes.isEmpty() }
        val base = "Lapp/morphe/extension/youtube/patches/FeedBindingRecovery"
        binder.interfaces.add(base + "\$Binder;")
        params.interfaces.add(base + "\$Position;")
        fun addMethod(owner: app.morphe.patcher.util.proxy.mutableTypes.MutableClass, name: String, parameters: List<String>, result: String, registers: Int, body: String) {
            owner.methods.add(ImmutableMethod(owner.type, name, parameters.map { ImmutableMethodParameter(it,null,null) },result,
                AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,null,null,MutableMethodImplementation(registers)).toMutable().apply { addInstructionsWithLabels(0,body) })
        }
        addMethod(binder,"patch_feedView",listOf(),"Landroid/view/View;",2,"""
            iget-object v0, p0, $listField
            return-object v0
        """)
        addMethod(binder,"patch_feedFullSpan",listOf("I"),"I",4,"""
            if-ltz p1, :unknown
            iget-object v0, p0, $itemsField
            invoke-interface {v0}, Ljava/util/List;->size()I
            move-result v1
            if-ge p1, v1, :unknown
            invoke-interface {v0, p1}, Ljava/util/List;->get(I)Ljava/lang/Object;
            move-result-object v0
            check-cast v0, $itemType
            invoke-virtual {v0}, $itemGetter
            move-result-object v0
            invoke-interface {v0}, $fullRef
            move-result v0
            return v0
            :unknown
            const/4 v0, -0x1
            return v0
        """)
        addMethod(binder,"patch_rebindFeed",listOf(),"V",2,"""
            iget-object v0, p0, $adapterField
            invoke-virtual {v0}, $notify
            return-void
        """)
        addMethod(params,"patch_feedPosition",listOf(),"I",2,"""
            invoke-virtual {p0}, $posGetter
            move-result v0
            return v0
        """)
        val width = binder.methods.single { it.name == widthRef.name && it.parameterTypes == widthRef.parameterTypes && it.returnType == "I" }
        width.addInstructions(0,"invoke-static/range {p0 .. p0}, $base;->observe(Ljava/lang/Object;)V")
    }
}
