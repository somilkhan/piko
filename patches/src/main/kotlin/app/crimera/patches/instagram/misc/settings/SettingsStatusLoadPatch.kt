/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.settings

import app.crimera.patches.instagram.misc.extension.hooks.instagramInitHook
import app.crimera.patches.instagram.utils.Constants.SSTS_DESCRIPTOR
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode

internal val settingsStatusLoadPatch = bytecodePatch {
    execute {
        instagramInitHook.fingerprint.method.apply {
            val firstInvokeSuperIndex = indexOfFirstInstruction(Opcode.INVOKE_SUPER)
            val loadCall = SSTS_DESCRIPTOR.format("load")
            if (instructions.any { it.toString() == loadCall }) return@apply
            addInstruction(firstInvokeSuperIndex + 1, loadCall)
        }
    }
}
