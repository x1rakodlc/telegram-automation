package org.telegram.messenger.automation;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class AutomationConfig {
    private static final String PREF_NAME = "tg_automation_config";

    // Keys
    private static final String KEY_ENABLED = "master_enabled";
    private static final String KEY_SELECTED_CHATS = "selected_chats"; // Set of Long as String
    private static final String KEY_ALLOWED_USER_IDS = "allowed_user_ids"; // comma separated
    private static final String KEY_TRIGGER_MODE_PREFIX = "chat_mode_"; // "all" or "keywords"
    private static final String KEY_KEYWORDS_PREFIX = "chat_keywords_"; // comma separated

    // Action toggle keys
    public static final String ACTION_VOLUME_UP = "act_volume_up";
    public static final String ACTION_VOLUME_DOWN = "act_volume_down";
    public static final String ACTION_HOME = "act_home";
    public static final String ACTION_BACK = "act_back";
    public static final String ACTION_RANDOM_TAP = "act_random_tap";
    public static final String ACTION_LOCK_SCREEN = "act_lock_screen";
    public static final String ACTION_UNLOCK_SCREEN = "act_unlock_screen";
    public static final String ACTION_FLASHLIGHT = "act_flashlight";
    public static final String ACTION_LAUNCH_APP = "act_launch_app";
    public static final String ACTION_SWIPE_UP = "act_swipe_up";
    public static final String ACTION_SWIPE_DOWN = "act_swipe_down";
    public static final String ACTION_SWIPE_LEFT = "act_swipe_left";
    public static final String ACTION_SWIPE_RIGHT = "act_swipe_right";

    public static final String[] ALL_ACTIONS = new String[]{
            ACTION_VOLUME_UP,
            ACTION_VOLUME_DOWN,
            ACTION_HOME,
            ACTION_BACK,
            ACTION_RANDOM_TAP,
            ACTION_LOCK_SCREEN,
            ACTION_UNLOCK_SCREEN,
            ACTION_FLASHLIGHT,
            ACTION_LAUNCH_APP,
            ACTION_SWIPE_UP,
            ACTION_SWIPE_DOWN,
            ACTION_SWIPE_LEFT,
            ACTION_SWIPE_RIGHT
    };

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isMasterEnabled() {
        return getPrefs().getBoolean(KEY_ENABLED, false);
    }

    public static void setMasterEnabled(boolean enabled) {
        getPrefs().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static Set<String> getSelectedChatIds() {
        return getPrefs().getStringSet(KEY_SELECTED_CHATS, new HashSet<>());
    }

    public static boolean isChatSelected(long chatId) {
        Set<String> chats = getSelectedChatIds();
        return chats.contains(String.valueOf(chatId));
    }

    public static void addChat(long chatId) {
        Set<String> set = new HashSet<>(getSelectedChatIds());
        set.add(String.valueOf(chatId));
        getPrefs().edit().putStringSet(KEY_SELECTED_CHATS, set).apply();
    }

    public static void removeChat(long chatId) {
        Set<String> set = new HashSet<>(getSelectedChatIds());
        set.remove(String.valueOf(chatId));
        getPrefs().edit().putStringSet(KEY_SELECTED_CHATS, set).apply();
    }

    public static void toggleChat(long chatId) {
        if (isChatSelected(chatId)) {
            removeChat(chatId);
        } else {
            addChat(chatId);
        }
    }

    // Trigger modes: "all" or "keywords"
    public static String getChatTriggerMode(long chatId) {
        return getPrefs().getString(KEY_TRIGGER_MODE_PREFIX + chatId, "all");
    }

    public static void setChatTriggerMode(long chatId, String mode) {
        getPrefs().edit().putString(KEY_TRIGGER_MODE_PREFIX + chatId, mode).apply();
    }

    public static String getChatKeywords(long chatId) {
        return getPrefs().getString(KEY_KEYWORDS_PREFIX + chatId, "");
    }

    public static void setChatKeywords(long chatId, String keywords) {
        getPrefs().edit().putString(KEY_KEYWORDS_PREFIX + chatId, keywords).apply();
    }

    public static String getAllowedUserIds() {
        return getPrefs().getString(KEY_ALLOWED_USER_IDS, "");
    }

    public static void setAllowedUserIds(String ids) {
        getPrefs().edit().putString(KEY_ALLOWED_USER_IDS, ids).apply();
    }

    public static boolean isUserAllowed(long userId) {
        String allowed = getAllowedUserIds().trim();
        if (TextUtils.isEmpty(allowed)) {
            return true; // Empty means allow all users
        }
        String[] parts = allowed.split(",");
        for (String p : parts) {
            try {
                if (Long.parseLong(p.trim()) == userId) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    public static boolean isActionEnabled(String actionKey) {
        // By default all actions are enabled
        return getPrefs().getBoolean(actionKey, true);
    }

    public static void setActionEnabled(String actionKey, boolean enabled) {
        getPrefs().edit().putBoolean(actionKey, enabled).apply();
    }

    public static String getRandomEnabledAction() {
        List<String> enabledList = new ArrayList<>();
        for (String act : ALL_ACTIONS) {
            if (isActionEnabled(act)) {
                enabledList.add(act);
            }
        }
        if (enabledList.isEmpty()) {
            return null;
        }
        Random rand = new Random();
        return enabledList.get(rand.nextInt(enabledList.size()));
    }
}
