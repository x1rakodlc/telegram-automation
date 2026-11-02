package org.telegram.messenger.automation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

public class AutomationForegroundService extends Service {
    private static final String CHANNEL_ID = "tg_automation_channel";
    private static final int NOTIFICATION_ID = 998877;
    public static final String ACTION_STOP = "org.telegram.messenger.automation.ACTION_STOP";
    public static final String ACTION_START = "org.telegram.messenger.automation.ACTION_START";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            AutomationConfig.setMasterEnabled(false);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Automation Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Telegram automation active in the background");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, AutomationForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, flags);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent contentPendingIntent = null;
        if (launchIntent != null) {
            contentPendingIntent = PendingIntent.getActivity(this, 0, launchIntent, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Telegram Automation Active")
                .setContentText("Monitoring selected chats for trigger actions")
                .setSmallIcon(R.drawable.notification)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .addAction(R.drawable.ic_close_white, "Turn Off", stopPendingIntent);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void updateServiceState(Context context) {
        try {
            if (AutomationConfig.isMasterEnabled()) {
                Intent intent = new Intent(context, AutomationForegroundService.class);
                intent.setAction(ACTION_START);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } else {
                Intent intent = new Intent(context, AutomationForegroundService.class);
                context.stopService(intent);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
