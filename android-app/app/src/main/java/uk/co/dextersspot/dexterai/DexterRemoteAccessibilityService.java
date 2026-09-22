package uk.co.dextersspot.dexterai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DexterRemoteAccessibilityService extends AccessibilityService {
    private static volatile DexterRemoteAccessibilityService instance;

    public static boolean isReady() {
        DexterRemoteAccessibilityService s = instance;
        return s != null && s.getRootInActiveWindow() != null;
    }

    public static DexterRemoteAccessibilityService get() {
        return instance;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override public void onInterrupt() {}

    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public JSONObject captureScreen() {
        JSONObject out = new JSONObject();
        if (Build.VERSION.SDK_INT < 30) {
            try { return out.put("connected", false).put("error", "Accessibility screenshot requires Android 11+."); }
            catch (Exception ignored) { return out; }
        }
        try {
            CountDownLatch latch = new CountDownLatch(1);
            final String[] b64 = {null};
            final String[] err = {null};
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult result) {
                    HardwareBuffer hb = result.getHardwareBuffer();
                    try {
                        ColorSpace cs = result.getColorSpace();
                        Bitmap wrapped = Bitmap.wrapHardwareBuffer(hb, cs);
                        if (wrapped == null) { err[0] = "Unable to wrap screenshot buffer."; return; }
                        Bitmap copy = wrapped.copy(Bitmap.Config.ARGB_8888, false);
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        copy.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                        b64[0] = Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
                        copy.recycle();
                    } catch (Exception e) {
                        err[0] = safe(e);
                    } finally {
                        try { hb.close(); } catch (Exception ignored) {}
                        latch.countDown();
                    }
                }
                @Override public void onFailure(int errorCode) {
                    err[0] = "Screenshot failed: " + errorCode;
                    latch.countDown();
                }
            });
            if (!latch.await(8, TimeUnit.SECONDS)) return out.put("connected", false).put("error", "Screenshot timed out.");
            if (b64[0] == null) return out.put("connected", false).put("error", err[0] == null ? "Screenshot unavailable." : err[0]);
            return out.put("connected", true).put("method", "accessibility").put("mimeType", "image/png").put("base64", b64[0]);
        } catch (Exception e) {
            try { return out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) { return out; }
        }
    }

    public JSONObject dumpUi() {
        JSONObject out = new JSONObject();
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return out.put("connected", false).put("error", "No active accessibility window.");
            StringBuilder xml = new StringBuilder();
            xml.append("<hierarchy>");
            appendNode(xml, root, 0);
            xml.append("</hierarchy>");
            return out.put("connected", true).put("method", "accessibility").put("xml", xml.toString());
        } catch (Exception e) {
            try { return out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) { return out; }
        }
    }

    public JSONObject tap(int x, int y) { return gesture(x, y, x, y, 80, "tap"); }

    public JSONObject swipe(int x1, int y1, int x2, int y2, int durationMs) {
        return gesture(x1, y1, x2, y2, Math.max(80, Math.min(durationMs, 5000)), "swipe");
    }

    private JSONObject gesture(int x1, int y1, int x2, int y2, int duration, String action) {
        JSONObject out = new JSONObject();
        try {
            Path p = new Path();
            p.moveTo(x1, y1);
            if (x1 != x2 || y1 != y2) p.lineTo(x2, y2);
            GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, duration)).build();
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] ok = {false};
            boolean queued = dispatchGesture(g, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gestureDescription) { ok[0] = true; latch.countDown(); }
                @Override public void onCancelled(GestureDescription gestureDescription) { latch.countDown(); }
            }, null);
            if (!queued) return out.put("connected", false).put("error", "Gesture was not accepted.");
            latch.await(6, TimeUnit.SECONDS);
            return out.put("connected", true).put("method", "accessibility").put("action", action).put("performed", ok[0]);
        } catch (Exception e) {
            try { return out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) { return out; }
        }
    }

    public JSONObject key(String key) {
        JSONObject out = new JSONObject();
        try {
            String k = key == null ? "" : key.trim().toUpperCase(java.util.Locale.ROOT);
            int action;
            if ("BACK".equals(k)) action = GLOBAL_ACTION_BACK;
            else if ("HOME".equals(k)) {
                if (Build.VERSION.SDK_INT >= 31) {
                    try { performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE); } catch (Exception ignored) {}
                }
                action = GLOBAL_ACTION_HOME;
            }
            else if ("APP_SWITCH".equals(k)) action = GLOBAL_ACTION_RECENTS;
            else if ("NOTIFICATIONS".equals(k)) action = GLOBAL_ACTION_NOTIFICATIONS;
            else if ("DISMISS_NOTIFICATIONS".equals(k)) {
                if (Build.VERSION.SDK_INT < 31) return out.put("connected", false).put("error", "Dismiss shade requires Android 12+.");
                boolean dismissed=performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
                return out.put("connected", true).put("method", "accessibility").put("action", "key").put("key", k).put("performed", dismissed);
            }
            else return out.put("connected", false).put("error", "Accessibility key unsupported: " + k);
            boolean ok = performGlobalAction(action);
            return out.put("connected", true).put("method", "accessibility").put("action", "key").put("key", k).put("performed", ok);
        } catch (Exception e) {
            try { return out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) { return out; }
        }
    }

    public JSONObject inputText(String text) {
        JSONObject out = new JSONObject();
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return out.put("connected", false).put("error", "No active accessibility window.");
            AccessibilityNodeInfo target = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (target == null || !target.isEditable()) return out.put("connected", false).put("error", "No editable field is focused.");
            Bundle b = new Bundle();
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text == null ? "" : text);
            boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
            return out.put("connected", true).put("method", "accessibility").put("action", "text").put("performed", ok);
        } catch (Exception e) {
            try { return out.put("connected", false).put("error", safe(e)); } catch (Exception ignored) { return out; }
        }
    }

    private static void appendNode(StringBuilder x, AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 40) return;
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        x.append("<node")
            .append(" class=\"").append(esc(n.getClassName())).append("\"")
            .append(" text=\"").append(esc(n.getText())).append("\"")
            .append(" content-desc=\"").append(esc(n.getContentDescription())).append("\"")
            .append(" view-id=\"").append(esc(n.getViewIdResourceName())).append("\"")
            .append(" clickable=\"").append(n.isClickable()).append("\"")
            .append(" editable=\"").append(n.isEditable()).append("\"")
            .append(" enabled=\"").append(n.isEnabled()).append("\"")
            .append(" bounds=\"[").append(r.left).append(",").append(r.top).append("][")
            .append(r.right).append(",").append(r.bottom).append("]\">");
        for (int i=0;i<n.getChildCount();i++) appendNode(x, n.getChild(i), depth+1);
        x.append("</node>");
    }

    private static String esc(Object v) {
        if (v == null) return "";
        return String.valueOf(v).replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String safe(Exception e) {
        String m=e.getMessage();
        return (m==null||m.isEmpty())?e.getClass().getSimpleName():m;
    }
}
