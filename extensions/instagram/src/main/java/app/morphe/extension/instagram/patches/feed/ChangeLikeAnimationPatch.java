/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
*/


package app.morphe.extension.instagram.patches.feed;

import app.morphe.extension.instagram.entity.Entity;
import android.graphics.drawable.Drawable;
import android.view.View;

import app.morphe.extension.instagram.utils.Pref;
import app.morphe.extension.shared.Logger;

public class ChangeLikeAnimationPatch {
    private static boolean checkPrefAnimation(String animation) {
        return animation == null || "ARES_LIKE_ACTIVATION".equals(animation);
    }

    public static Object changeLikeAnimation(Object defaultAnimation){
        try {
            String animation = Pref.changeLikeAnimation();

            if (CustomLikeAnimationStore.isCustomSelection(animation)) {
                Entity entity = new Entity();
                Class<?> animationEnumClass = Class.forName("className");
                return entity.getMethod(
                        animationEnumClass,
                        "valueOf",
                        CustomLikeAnimationStore.CARRIER_ANIMATION
                );
            }

            if (ChangeLikeAnimationPatch.checkPrefAnimation(animation)) {
                return null;
            }

            Entity entity = new Entity();
            Class<?> animationEnumClass = Class.forName("className");
            Object likeAnimation = entity.getMethod(
                    animationEnumClass,
                    "valueOf",
                    animation
            );
            return likeAnimation;

        } catch (Exception e) {
            Logger.printException(() -> "changeLikeAnimation failure", e);
        }
        return defaultAnimation;
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