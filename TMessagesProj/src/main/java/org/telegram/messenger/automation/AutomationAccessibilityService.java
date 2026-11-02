package org.telegram.messenger.automation;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutomationAccessibilityService extends AccessibilityService {

    private static AutomationAccessibilityService instance;
    private static boolean isFlashlightOn = false;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        FileLog.d("AutomationAccessibilityService connected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used for active listening, service is primarily used for dispatching actions
    }

    @Override
    public void onInterrupt() {
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static AutomationAccessibilityService getInstance() {
        return instance;
    }

    public void executeRandomAction() {
        String action = AutomationConfig.getRandomEnabledAction();
        if (action == null) {
            FileLog.d("Automation: No action is enabled");
            return;
        }
        FileLog.d("Automation: Executing action -> " + action);

        switch (action) {
            case AutomationConfig.ACTION_VOLUME_UP:
                adjustVolume(AudioManager.ADJUST_RAISE);
                break;
            case AutomationConfig.ACTION_VOLUME_DOWN:
                adjustVolume(AudioManager.ADJUST_LOWER);
                break;
            case AutomationConfig.ACTION_HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case AutomationConfig.ACTION_BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case AutomationConfig.ACTION_LOCK_SCREEN:
                lockScreen();
                break;
            case AutomationConfig.ACTION_UNLOCK_SCREEN:
                wakeScreen();
                break;
            case AutomationConfig.ACTION_FLASHLIGHT:
                toggleFlashlight();
                break;
            case AutomationConfig.ACTION_LAUNCH_APP:
                launchRandomApp();
                break;
            case AutomationConfig.ACTION_RANDOM_TAP:
                performRandomTap();
                break;
            case AutomationConfig.ACTION_SWIPE_UP:
                swipe(SwipeDirection.UP);
                break;
            case AutomationConfig.ACTION_SWIPE_DOWN:
                swipe(SwipeDirection.DOWN);
                break;
            case AutomationConfig.ACTION_SWIPE_LEFT:
                swipe(SwipeDirection.LEFT);
                break;
            case AutomationConfig.ACTION_SWIPE_RIGHT:
                swipe(SwipeDirection.RIGHT);
                break;
        }
    }

    private void adjustVolume(int direction) {
        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void lockScreen() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void wakeScreen() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                @SuppressWarnings("deprecation")
                PowerManager.WakeLock wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "Telegram:AutomationWakeLock"
                );
                wakeLock.acquire(3000);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void toggleFlashlight() {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cm != null) {
                String[] cameraIds = cm.getCameraIdList();
                for (String id : cameraIds) {
                    try {
                        isFlashlightOn = !isFlashlightOn;
                        cm.setTorchMode(id, isFlashlightOn);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void launchRandomApp() {
        try {
            PackageManager pm = getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
            List<ResolveInfo> launchable = new ArrayList<>();
            String myPkg = getPackageName();
            for (ResolveInfo ri : apps) {
                if (ri.activityInfo != null && !myPkg.equals(ri.activityInfo.packageName)) {
                    launchable.add(ri);
                }
            }
            if (!launchable.isEmpty()) {
                ResolveInfo chosen = launchable.get(new Random().nextInt(launchable.size()));
                Intent launchIntent = pm.getLaunchIntentForPackage(chosen.activityInfo.packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void performRandomTap() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            List<AccessibilityNodeInfo> clickableNodes = new ArrayList<>();
            findClickableNodes(root, clickableNodes);

            if (!clickableNodes.isEmpty()) {
                AccessibilityNodeInfo target = clickableNodes.get(new Random().nextInt(clickableNodes.size()));
                Rect rect = new Rect();
                target.getBoundsInScreen(rect);
                if (rect.width() > 0 && rect.height() > 0) {
                    clickAtPoint(rect.centerX(), rect.centerY());
                } else {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            } else {
                // If no clickable nodes found, tap random screen location
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int x = new Random().nextInt(dm.widthPixels);
                int y = new Random().nextInt(dm.heightPixels);
                clickAtPoint(x, y);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void findClickableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> list) {
        if (node == null) return;
        if (node.isClickable() && node.isVisibleToUser()) {
            list.add(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            findClickableNodes(node.getChild(i), list);
        }
    }

    private void clickAtPoint(float x, float y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(clickPath, 0, 50);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            dispatchGesture(builder.build(), null, null);
        }
    }

    public enum SwipeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    private void swipe(SwipeDirection direction) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int width = dm.widthPixels;
            int height = dm.heightPixels;

            float startX = width / 2.0f;
            float startY = height / 2.0f;
            float endX = startX;
            float endY = startY;

            switch (direction) {
                case UP:
                    startY = height * 0.75f;
                    endY = height * 0.25f;
                    break;
                case DOWN:
                    startY = height * 0.25f;
                    endY = height * 0.75f;
                    break;
                case LEFT:
                    startX = width * 0.8f;
                    endX = width * 0.2f;
                    break;
                case RIGHT:
                    startX = width * 0.2f;
                    endX = width * 0.8f;
                    break;
            }

            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 300);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
