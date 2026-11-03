package org.telegram.ui.automation;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.automation.AutomationAccessibilityService;
import org.telegram.messenger.automation.AutomationConfig;
import org.telegram.messenger.automation.AutomationForegroundService;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Set;

public class AutomationSettingsActivity extends BaseFragment {

    private UniversalRecyclerView listView;

    private static final int ID_MASTER = 1;
    private static final int ID_ACCESSIBILITY_STATUS = 2;
    private static final int ID_ALLOWED_USERS = 3;

    // Actions IDs 101 - 113
    private static final int ID_ACT_VOLUME_UP = 101;
    private static final int ID_ACT_VOLUME_DOWN = 102;
    private static final int ID_ACT_HOME = 103;
    private static final int ID_ACT_BACK = 104;
    private static final int ID_ACT_RANDOM_TAP = 105;
    private static final int ID_ACT_LOCK_SCREEN = 106;
    private static final int ID_ACT_UNLOCK_SCREEN = 107;
    private static final int ID_ACT_FLASHLIGHT = 108;
    private static final int ID_ACT_LAUNCH_APP = 109;
    private static final int ID_ACT_SWIPE_UP = 110;
    private static final int ID_ACT_SWIPE_DOWN = 111;
    private static final int ID_ACT_SWIPE_LEFT = 112;
    private static final int ID_ACT_SWIPE_RIGHT = 113;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Device Automation");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = listView = new UniversalRecyclerView(this, this::fillItems, this::onClick, this::onLongClick);
        return fragmentView;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("General"));
        items.add(UItem.asCheck(ID_MASTER, "Enable Automation").setChecked(AutomationConfig.isMasterEnabled()));

        boolean isAccRunning = AutomationAccessibilityService.isRunning();
        items.add(UItem.asButton(ID_ACCESSIBILITY_STATUS, "Accessibility Service", isAccRunning ? "Active" : "Tap to Grant / Open Settings"));

        String userIds = AutomationConfig.getAllowedUserIds();
        items.add(UItem.asButton(ID_ALLOWED_USERS, "Filter Sender User IDs", userIds.isEmpty() ? "All users (default)" : userIds));

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Selected Chats (" + AutomationConfig.getSelectedChatIds().size() + ")"));

        Set<String> selectedChats = AutomationConfig.getSelectedChatIds();
        if (selectedChats.isEmpty()) {
            items.add(UItem.asButton(0, "No chats selected", "Open any chat > tap 3 dots > Enable Automation"));
        } else {
            int chatIndex = 1000;
            for (String chatIdStr : selectedChats) {
                try {
                    long chatId = Long.parseLong(chatIdStr);
                    String title = "Chat ID: " + chatId;
                    if (chatId > 0) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(chatId);
                        if (user != null) {
                            title = user.first_name + (user.last_name != null ? " " + user.last_name : "");
                        }
                    } else {
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-chatId);
                        if (chat != null) {
                            title = chat.title;
                        }
                    }
                    String mode = AutomationConfig.getChatTriggerMode(chatId);
                    String sub = "all".equalsIgnoreCase(mode) ? "Trigger: Any message (Tap to edit / Long tap to remove)" : "Trigger: Keywords [" + AutomationConfig.getChatKeywords(chatId) + "]";
                    UItem chatItem = UItem.asButton(chatIndex++, title, sub);
                    chatItem.object = chatId;
                    items.add(chatItem);
                } catch (Exception ignored) {
                }
            }
        }

        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader("Random Actions (Choose which to perform)"));

        items.add(UItem.asCheck(ID_ACT_VOLUME_UP, "Volume Up").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_VOLUME_UP)));
        items.add(UItem.asCheck(ID_ACT_VOLUME_DOWN, "Volume Down").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_VOLUME_DOWN)));
        items.add(UItem.asCheck(ID_ACT_HOME, "Home (Global Action)").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_HOME)));
        items.add(UItem.asCheck(ID_ACT_BACK, "Back (Global Action)").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_BACK)));
        items.add(UItem.asCheck(ID_ACT_RANDOM_TAP, "Random UI Tap").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_RANDOM_TAP)));
        items.add(UItem.asCheck(ID_ACT_LOCK_SCREEN, "Lock Screen (Sleep)").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_LOCK_SCREEN)));
        items.add(UItem.asCheck(ID_ACT_UNLOCK_SCREEN, "Unlock / Wake Screen").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_UNLOCK_SCREEN)));
        items.add(UItem.asCheck(ID_ACT_FLASHLIGHT, "Toggle Flashlight").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_FLASHLIGHT)));
        items.add(UItem.asCheck(ID_ACT_LAUNCH_APP, "Launch Random App").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_LAUNCH_APP)));
        items.add(UItem.asCheck(ID_ACT_SWIPE_UP, "Swipe Up").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_SWIPE_UP)));
        items.add(UItem.asCheck(ID_ACT_SWIPE_DOWN, "Swipe Down").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_SWIPE_DOWN)));
        items.add(UItem.asCheck(ID_ACT_SWIPE_LEFT, "Swipe Left").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_SWIPE_LEFT)));
        items.add(UItem.asCheck(ID_ACT_SWIPE_RIGHT, "Swipe Right").setChecked(AutomationConfig.isActionEnabled(AutomationConfig.ACTION_SWIPE_RIGHT)));
        items.add(UItem.asShadow(null));
    }

    private void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_MASTER) {
            boolean current = AutomationConfig.isMasterEnabled();
            AutomationConfig.setMasterEnabled(!current);
            AutomationForegroundService.updateServiceState(getParentActivity());
            listView.adapter.update(true);
            return;
        }

        if (item.id == ID_ACCESSIBILITY_STATUS) {
            try {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                if (getParentActivity() != null) {
                    getParentActivity().startActivity(intent);
                }
            } catch (Exception e) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "Cannot open accessibility settings").show();
            }
            return;
        }

        if (item.id == ID_ALLOWED_USERS) {
            showAllowedUsersDialog();
            return;
        }

        if (item.id >= 1000 && item.object instanceof Long) {
            showChatConfigDialog((Long) item.object);
            return;
        }

        // Action toggles
        String actionKey = null;
        switch (item.id) {
            case ID_ACT_VOLUME_UP: actionKey = AutomationConfig.ACTION_VOLUME_UP; break;
            case ID_ACT_VOLUME_DOWN: actionKey = AutomationConfig.ACTION_VOLUME_DOWN; break;
            case ID_ACT_HOME: actionKey = AutomationConfig.ACTION_HOME; break;
            case ID_ACT_BACK: actionKey = AutomationConfig.ACTION_BACK; break;
            case ID_ACT_RANDOM_TAP: actionKey = AutomationConfig.ACTION_RANDOM_TAP; break;
            case ID_ACT_LOCK_SCREEN: actionKey = AutomationConfig.ACTION_LOCK_SCREEN; break;
            case ID_ACT_UNLOCK_SCREEN: actionKey = AutomationConfig.ACTION_UNLOCK_SCREEN; break;
            case ID_ACT_FLASHLIGHT: actionKey = AutomationConfig.ACTION_FLASHLIGHT; break;
            case ID_ACT_LAUNCH_APP: actionKey = AutomationConfig.ACTION_LAUNCH_APP; break;
            case ID_ACT_SWIPE_UP: actionKey = AutomationConfig.ACTION_SWIPE_UP; break;
            case ID_ACT_SWIPE_DOWN: actionKey = AutomationConfig.ACTION_SWIPE_DOWN; break;
            case ID_ACT_SWIPE_LEFT: actionKey = AutomationConfig.ACTION_SWIPE_LEFT; break;
            case ID_ACT_SWIPE_RIGHT: actionKey = AutomationConfig.ACTION_SWIPE_RIGHT; break;
        }
        if (actionKey != null) {
            boolean current = AutomationConfig.isActionEnabled(actionKey);
            AutomationConfig.setActionEnabled(actionKey, !current);
            listView.adapter.update(true);
        }
    }

    private boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 1000 && item.object instanceof Long) {
            long chatId = (Long) item.object;
            AutomationConfig.removeChat(chatId);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Chat removed from automation").show();
            return true;
        }
        return false;
    }

    private void showAllowedUsersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Allowed User IDs");
        builder.setMessage("Enter comma-separated Telegram User IDs to react to, or leave empty to react to all users:");

        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(AutomationConfig.getAllowedUserIds());
        builder.setView(input);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            AutomationConfig.setAllowedUserIds(input.getText().toString());
            listView.adapter.update(true);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showChatConfigDialog(long chatId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Configure Chat Trigger");

        String currentMode = AutomationConfig.getChatTriggerMode(chatId);
        CharSequence[] options = new CharSequence[]{
                "React to All Messages" + ("all".equalsIgnoreCase(currentMode) ? " (Active)" : ""),
                "React to Keywords" + ("keywords".equalsIgnoreCase(currentMode) ? " (Active)" : "")
        };

        builder.setItems(options, (dialog, which) -> {
            dialog.dismiss();
            if (which == 0) {
                AutomationConfig.setChatTriggerMode(chatId, "all");
                listView.adapter.update(true);
            } else {
                AutomationConfig.setChatTriggerMode(chatId, "keywords");
                showKeywordsDialog(chatId);
            }
        });
        builder.setNegativeButton("Remove Chat", (dialog, which) -> {
            AutomationConfig.removeChat(chatId);
            listView.adapter.update(true);
        });
        showDialog(builder.create());
    }

    private void showKeywordsDialog(long chatId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Enter Keywords");
        builder.setMessage("Enter comma-separated trigger keywords (case-insensitive):");

        final EditText input = new EditText(getParentActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(AutomationConfig.getChatKeywords(chatId));
        builder.setView(input);

        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            AutomationConfig.setChatKeywords(chatId, input.getText().toString());
            listView.adapter.update(true);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }
}
