/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
*/


package app.morphe.extension.instagram.settings.preference.widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import app.morphe.extension.instagram.settings.Settings;
import app.morphe.extension.shared.ResourceUtils;
import android.preference.Preference;
import app.morphe.extension.instagram.settings.preference.Helper;
import app.morphe.extension.instagram.settings.SettingsActivity;
import app.morphe.extension.instagram.patches.feed.CustomLikeAnimationStore;
import static app.morphe.extension.instagram.utils.IgStr.str;

public class ListPref extends ListPreference {
    private static Helper helper;
    private Context activityContext;

    public ListPref(Context context) {
        super(InstagramPreferenceStyle.dialogContext(context));
        activityContext = context;
        helper = new Helper(context);
        init();
    }

    public ListPref(Context context, AttributeSet attrs) {
        super(context, attrs);
        activityContext = context;
        helper = new Helper(context);
        init();
    }

    public ListPref(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activityContext = context;
        helper = new Helper(context);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                helper.setValue(preference,newValue);
                return true;
            }
        });
    }

    private void loadEntries() {
        String key = getKey();
        CharSequence[] entries = new CharSequence[]{};
        CharSequence[] entriesValues = new CharSequence[]{};

        if (key != null && key.contains("_")) {
            if (Settings.CUSTOMISE_STORY_TIMESTAMP.key.equals(key)) {
                entries = ResourceUtils.getStringArray("piko_array_customise_story_timestamp");
                entriesValues = ResourceUtils.getStringArray("piko_array_customise_story_timestamp_val");
            } else if (Settings.CHANGE_LIKE_ANIMATION.key.equals(key)) {
                CharSequence[] builtInEntries =
                        ResourceUtils.getStringArray("piko_array_change_like_animation");
                CharSequence[] builtInValues =
                        ResourceUtils.getStringArray("piko_array_change_like_animation_val");
                java.util.List<CustomLikeAnimationStore.CustomAnimation> customAnimations =
                        CustomLikeAnimationStore.getAnimations();

                entries = new CharSequence[builtInEntries.length + customAnimations.size()];
                entriesValues = new CharSequence[builtInValues.length + customAnimations.size()];
                System.arraycopy(builtInEntries, 0, entries, 0, builtInEntries.length);
                System.arraycopy(builtInValues, 0, entriesValues, 0, builtInValues.length);

                for (int i = 0; i < customAnimations.size(); i++) {
                    CustomLikeAnimationStore.CustomAnimation animation = customAnimations.get(i);
                    entries[builtInEntries.length + i] = animation.name;
                    entriesValues[builtInValues.length + i] =
                            CustomLikeAnimationStore.selectionFor(animation.id);
                }
            }
        } else {
            entries = ResourceUtils.getStringArray("piko_array_recmd_flag_states");
            entriesValues = ResourceUtils.getStringArray("piko_array_recmd_flag_states_val");
        }

        setEntries(entries);
        setEntryValues(entriesValues);
    }

    public void reloadEntries() {
        loadEntries();
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        loadEntries();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (Settings.CHANGE_LIKE_ANIMATION.key.equals(getKey())) {
            loadEntries();
        }
        super.onPrepareDialogBuilder(builder);

        if (Settings.CHANGE_LIKE_ANIMATION.key.equals(getKey())) {
            builder.setNeutralButton(
                    str("piko_import_custom_like_animation"),
                    (dialog, which) -> {
                        if (activityContext instanceof SettingsActivity) {
                            ((SettingsActivity) activityContext).openCustomLikeAnimationPicker();
                        } else if (activityContext instanceof Activity) {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("*/*");
                            ((Activity) activityContext).startActivityForResult(
                                    intent, SettingsActivity.REQUEST_IMPORT_CUSTOM_LIKE_ANIMATION);
                        }
                    }
            );
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        return InstagramPreferenceStyle.createPreferenceView(getContext(), InstagramPreferenceStyle.TRAILING_CHEVRON);
    }

    @Override
    protected void onBindView(View view) {
        InstagramPreferenceStyle.bindText(this, view);
    }
}
