/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.utils

import app.crimera.patches.instagram.misc.extension.hooks.instagramInitHook
import app.crimera.patches.instagram.misc.settings.HookFlagsLoadFingerprint
import app.crimera.patches.instagram.misc.settings.SettingsStatusLoadFingerprint
import app.crimera.patches.instagram.utils.Constants.LOAD_FLAGS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.SSTS_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.patch.BytecodePatchContext

context(patchContext: BytecodePatchContext)
fun ensureSettingsStatusLoad() {
    val method = instagramInitHook.fingerprint.method
    val loadCall = SSTS_DESCRIPTOR.format("load")
    if (method.instructions.any { it.toString() == loadCall }) return

    val firstInvokeSuperIndex = method.indexOfFirstInstruction(Opcode.INVOKE_SUPER)
    method.addInstruction(firstInvokeSuperIndex + 1, loadCall)
}

context(patchContext: BytecodePatchContext)
fun enableSettings(functionName: String) {
    ensureSettingsStatusLoad()

    val method = instagramInitHook.fingerprint.method
    val loadCall = SSTS_DESCRIPTOR.format("load")
    val loadIndex = method.instructions.indexOfFirst {
        it.toString() == loadCall
    }
    check(loadIndex >= 0) {
        "SettingsStatus.load() call could not be installed: $functionName"
    }

    method.addInstruction(
        loadIndex,
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
