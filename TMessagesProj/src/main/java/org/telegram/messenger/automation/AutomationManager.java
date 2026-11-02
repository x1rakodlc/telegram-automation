package org.telegram.messenger.automation;

import android.text.TextUtils;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.util.Locale;

public class AutomationManager {

    public static void onNewMessageReceived(long dialogId, TLRPC.Message message) {
        if (!AutomationConfig.isMasterEnabled()) {
            return;
        }

        if (!AutomationConfig.isChatSelected(dialogId)) {
            return;
        }

        if (message == null) {
            return;
        }

        // Check user id if filter specified
        long senderUserId = 0;
        if (message.from_id != null) {
            if (message.from_id instanceof TLRPC.TL_peerUser) {
                senderUserId = message.from_id.user_id;
            } else if (message.from_id instanceof TLRPC.TL_peerChannel) {
                senderUserId = -message.from_id.channel_id;
            } else if (message.from_id instanceof TLRPC.TL_peerChat) {
                senderUserId = -message.from_id.chat_id;
            }
        }
        if (senderUserId != 0 && !AutomationConfig.isUserAllowed(senderUserId)) {
            FileLog.d("Automation: Message from sender " + senderUserId + " ignored by user filter");
            return;
        }

        // Check trigger mode
        String mode = AutomationConfig.getChatTriggerMode(dialogId);
        boolean shouldTrigger = false;

        if ("keywords".equalsIgnoreCase(mode)) {
            String keywords = AutomationConfig.getChatKeywords(dialogId);
            if (!TextUtils.isEmpty(keywords) && message.message != null) {
                String text = message.message.toLowerCase(Locale.ROOT);
                String[] parts = keywords.split(",");
                for (String kw : parts) {
                    String cleanKw = kw.trim().toLowerCase(Locale.ROOT);
                    if (!cleanKw.isEmpty() && text.contains(cleanKw)) {
                        shouldTrigger = true;
                        break;
                    }
                }
            }
        } else {
            // "all" mode: triggers on any message
            shouldTrigger = true;
        }

        if (shouldTrigger) {
            FileLog.d("Automation: Trigger matched for dialog " + dialogId + "! Executing random action...");
            AutomationAccessibilityService service = AutomationAccessibilityService.getInstance();
            if (service != null) {
                service.executeRandomAction();
            } else {
                FileLog.d("Automation: Accessibility service is not running or not enabled");
            }
        }
    }
}
