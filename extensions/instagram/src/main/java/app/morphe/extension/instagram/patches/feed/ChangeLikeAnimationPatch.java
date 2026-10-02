/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
*/

package app.morphe.extension.instagram.patches.feed;

import android.graphics.drawable.Drawable;
import android.view.View;

import app.morphe.extension.instagram.entity.Entity;
import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.shared.Logger;

public class ChangeLikeAnimationPatch {
    private static final String DEFAULT = "ARES_LIKE_ACTIVATION";

    public static Object changeLikeAnimation(Object defaultAnimation) {
        try {
            String animation = Pref.changeLikeAnimation();
            if (animation == null || DEFAULT.equals(animation)) {
                return null;
            }
            if (CustomLikeAnimationStore.isCustomSelection(animation)) {
                return null;
            }

            Entity entity = new Entity();
            Class<?> animationEnumClass = Class.forName("className");
            return entity.getMethod(animationEnumClass, "valueOf", animation);
        } catch (Exception e) {
            Logger.printException(() -> "changeLikeAnimation failure", e);
            return defaultAnimation;
        }
    }

    public static Object changeRenderAnimation(Object defaultAnimation) {
        try {
            String animation = Pref.changeLikeAnimation();
            if (animation == null || DEFAULT.equals(animation)) {
                return null;
            }
            if (CustomLikeAnimationStore.isCustomSelection(animation)) {
                return defaultAnimation;
            }

            Entity entity = new Entity();
            Class<?> animationEnumClass = Class.forName("className");
            Object selected = entity.getMethod(animationEnumClass, "valueOf", animation);
            return mapAnimation(selected);
        } catch (Exception e) {
            Logger.printException(() -> "changeRenderAnimation failure", e);
            return defaultAnimation;
        }
    }

    private static Object mapAnimation(Object animation) {
        return animation;
    }

    public static Drawable createCustomLikeAnimationDrawable(Object view) {
        if (!(view instanceof View)) return null;
        try {
            return CustomLikeAnimationStore.createDrawableForCurrentSelection(
                    ((View) view).getContext()
            );
        } catch (Exception ignored) {
            return null;
        }
    }
}
