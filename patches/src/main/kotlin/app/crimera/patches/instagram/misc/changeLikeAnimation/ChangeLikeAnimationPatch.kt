/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.changeLikeAnimation

import app.crimera.patches.instagram.misc.settings.SettingsStatusLoadFingerprint
import app.crimera.patches.instagram.misc.settings.settingsPatch
import app.crimera.patches.instagram.utils.Constants.COMPATIBILITY_INSTAGRAM
import app.crimera.patches.instagram.utils.Constants.PATCHES_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.SSTS_DESCRIPTOR
import app.crimera.patches.instagram.utils.Constants.USER_SESSION_CLASS
import app.crimera.utils.changeFirstString
import app.crimera.utils.classNameToExtension
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.all.misc.resources.addAppResources
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "$PATCHES_DESCRIPTOR/feed/ChangeLikeAnimationPatch;"
private const val LIKE_VIEW = "Lcom/instagram/ui/mediaactions/LikeActionView;"
private const val CONTEXT = "Landroid/content/Context;"

internal object ChangeLikeAnimationExtensionFingerprint : Fingerprint(
    name = "changeLikeAnimation",
    definingClass = EXTENSION_CLASS_DESCRIPTOR,
)

internal object LikeActionViewSetUpCustomLikesAnimationFingerprint : Fingerprint(
    name = "setUpCustomLikesAnimation",
    definingClass = LIKE_VIEW,
)

internal object MapAnimationExtensionFingerprint : Fingerprint(
    name = "mapAnimation",
    definingClass = EXTENSION_CLASS_DESCRIPTOR,
)

internal object XDTUserActivationMetadataImplInitFingerprint : Fingerprint(
    name = "<init>",
    definingClass = "Lcom/instagram/api/schemas/XDTUserActivationMetadataImpl;",
)

context(context: BytecodePatchContext)
private fun installAnimationRendering(animationType: String) {
    val setup = LikeActionViewSetUpCustomLikesAnimationFingerprint.method
    val renderType = setup.parameterTypes.singleOrNull()?.toString()
        ?: throw PatchException("Unexpected custom like animation setup signature")

    val renderClass = context.classDefBy(renderType)
    if (renderClass.superclass != "Ljava/lang/Enum;") {
        throw PatchException("Expected a like animation rendering enum")
    }

    val mapper = renderClass.methods.singleOrNull {
        it.parameterTypes == listOf(animationType) &&
            it.returnType == renderType &&
            AccessFlags.PUBLIC.isSet(it.accessFlags) &&
            AccessFlags.STATIC.isSet(it.accessFlags)
    } ?: throw PatchException("Expected one like animation enum mapper")

    val configure = Fingerprint(
        definingClass = LIKE_VIEW,
        parameters = listOf(USER_SESSION_CLASS, renderType),
        returnType = "V",
    ).matchAll(0..Int.MAX_VALUE).singleOrNull()?.method
        ?: throw PatchException("Expected one like animation view configuration method")

    if (configure.instructions.none {
            it.getReference<MethodReference>()?.toString() == setup.toString()
        }) {
        throw PatchException("Like animation view configuration does not call custom animation setup")
    }

    configure.addInstructions(
        0,
        """
        invoke-static/range {p2 .. p2}, $EXTENSION_CLASS_DESCRIPTOR->changeRenderAnimation(Ljava/lang/Object;)Ljava/lang/Object;
        move-result-object p2
        check-cast p2, $renderType
        """.trimIndent(),
    )

    MapAnimationExtensionFingerprint.method.addInstructions(
        0,
        """
        check-cast p0, $animationType
        invoke-static/range {p0 .. p0}, $mapper
        move-result-object p0
        return-object p0
        """.trimIndent(),
    )
}

@Suppress("unused")
val changeLikeAnimationPatch =
    bytecodePatch(
        name = "Change like animation",
        description = "Change the animation to one from existing Rings like animations",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_INSTAGRAM)
        dependsOn(settingsPatch)

        execute {
            addAppResources("shared")
            addAppResources("instagram")

            val animationType = XDTUserActivationMetadataImplInitFingerprint.method.parameters[0].type
            ChangeLikeAnimationExtensionFingerprint.changeFirstString(
                classNameToExtension(animationType)
            )

            installAnimationRendering(animationType)

            // This flag controls whether the setting is exposed in Piko Settings.
            // Keep the explicit startup registration; SettingsActivity also enables the
            // capability before the fragment is built.
            SettingsStatusLoadFingerprint.method.addInstruction(
                0,
                SSTS_DESCRIPTOR.format("changeLikeAnimation"),
            )
        }

        execute {
            LikeActionViewSetUpCustomLikesAnimationFingerprint.method.apply {
                addInstructionsWithLabels(
                    0,
                    """
                    invoke-static {p0}, $EXTENSION_CLASS_DESCRIPTOR->createCustomLikeAnimationDrawable(Ljava/lang/Object;)Landroid/graphics/drawable/Drawable;
                    move-result-object v0
                    if-eqz v0, :piko_original_like_animation
                    iget-object v1, p0, Lcom/instagram/ui/mediaactions/LikeActionView;->A00:LX/06GF;
                    invoke-virtual {v1, v0}, Landroid/widget/ImageView;->setImageDrawable(Landroid/graphics/drawable/Drawable);
                    goto :piko_continue_like_animation
                    """.trimIndent(),
                    ExternalLabel("piko_original_like_animation", getInstruction(0)),
                    ExternalLabel("piko_continue_like_animation", getInstruction(9)),
                )
            }
        }
    }
