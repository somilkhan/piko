/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
*/


package app.morphe.extension.instagram.patches.feed;

import app.morphe.extension.instagram.entity.Entity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    /**
     * Applies an imported animation after Instagram has completed its native
     * LikeActionView setup. This intentionally does not assume that the hooked
     * object itself is an ImageView.
     */
    public static void applyCustomLikeAnimation(Object view) {
        if (!(view instanceof View)) return;

        try {
            Drawable drawable = CustomLikeAnimationStore.createDrawableForCurrentSelection(
                    ((View) view).getContext()
            );
            if (drawable == null) return;

            ImageView target = findImageView((View) view);
            if (target != null) {
                target.setImageDrawable(drawable);
            }
        } catch (Exception e) {
            Logger.printException(() -> "custom like animation apply failed", e);
        }
    }

    private static ImageView findImageView(View view) {
        if (view instanceof ImageView) {
            return (ImageView) view;
        }

        if (!(view instanceof ViewGroup)) return null;

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ImageView target = findImageView(group.getChildAt(i));
            if (target != null) return target;
        }
        return null;
    }
}
