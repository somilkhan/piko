/* Copyright (C) 2026 piko <https://github.com/crimera/piko> */
package app.crimera.patches.instagram.utils

import app.crimera.patches.instagram.misc.extension.hooks.instagramInitHook
import app.crimera.patches.instagram.misc.settings.HookFlagsLoadFingerprint
import app.crimera.patches.instagram.misc.settings.SettingsStatusLoadFingerprint
import app.crimera.patches.instagram.utils.Constants.LOAD_FLAGS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.SSTS_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode

context(patchContext: BytecodePatchContext)
fun ensureSettingsStatusLoad() {
    instagramInitHook.fingerprint.method.apply {
        val firstInvokeSuperIndex = indexOfFirstInstruction(Opcode.INVOKE_SUPER)
        addInstruction(firstInvokeSuperIndex + 1, SSTS_DESCRIPTOR.format("load"))
    }
}

context(patchContext: BytecodePatchContext)
fun enableSettings(functionName: String) {
    SettingsStatusLoadFingerprint.method.addInstruction(
        0,
        SSTS_DESCRIPTOR.format(functionName),
    )
}

context(patchContext: BytecodePatchContext)
fun addFlags(functionName: String) {
    HookFlagsLoadFingerprint.method.addInstruction(
        0,
        LOAD_FLAGS_DESCRIPTOR.format(functionName),
    )
}
