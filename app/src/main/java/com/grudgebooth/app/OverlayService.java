package com.grudgebooth.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ImageView;

/**
 * Draws the mess on top of whatever else is on screen.
 *
 * Two windows. The big one is a transparent WebView running overlay.html and is
 * NOT_TOUCHABLE by default, so Instagram (or anything else) scrolls exactly as
 * it normally would and the splats just sit in front. The small one is a
 * draggable bubble that stays touchable; tapping it flips the big window to
 * touchable for a moment so taps land as throws.
 *
 * It has to be a mode. An overlay either receives a touch or lets it through -
 * Android has no way to do both, and nothing lets one app hand a touch to
 * another. So: tap the bubble, throw a few, and it hands scrolling back on its
 * own after IDLE_RELEASE_MS of nothing.
 */
public class OverlayService extends Service {

    public static final String ACTION_STOP = "com.grudgebooth.app.STOP_OVERLAY";
    private static final String CHANNEL = "overlay";
    private static final int NOTE_ID = 42;
    /** hand scrolling back this long after the last throw */
    private static final long IDLE_RELEASE_MS = 1500L;

    private WindowManager wm;
    private WebView web;
    private ImageView bubble;
    private WindowManager.LayoutParams webLp, bubbleLp;
    private boolean live = false;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable release = new Runnable() {
        @Override public void run() { setLive(false); }
    };

    @Override public IBinder onBind(Intent i) { return null; }

    /** True when the user has granted "display over other apps". */
    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(c);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForeground(NOTE_ID, note());

        if (!allowed(this)) { stopSelf(); return; }

        int type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // --- the mess, edge to edge, transparent, ignoring touches for now ---
        web = new WebView(this);
        web.setBackgroundColor(0x00000000);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.addJavascriptInterface(new OverlayBridge(), "GrudgeOverlay");
        web.loadUrl("file:///android_asset/overlay.html");

        webLp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                passThroughFlags(),
                PixelFormat.TRANSLUCENT);
        webLp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            webLp.layoutInDisplayCutoutMode = WindowManager.LayoutParams
                    .LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        wm.addView(web, webLp);

        // --- the bubble: small, always touchable, draggable ---
        bubble = new ImageView(this);
        bubble.setImageResource(R.drawable.ic_bubble);
        int px = Math.round(getResources().getDisplayMetrics().density * 56);
        bubbleLp = new WindowManager.LayoutParams(px, px, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        bubbleLp.gravity = Gravity.TOP | Gravity.START;
        bubbleLp.x = 24;
        bubbleLp.y = Math.round(getResources().getDisplayMetrics().heightPixels * 0.62f);
        bubble.setOnTouchListener(new BubbleTouch());
        wm.addView(bubble, bubbleLp);
    }

    private int passThroughFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
    }

    private int catchFlags() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
    }

    /** Flip between "taps reach the app underneath" and "taps are throws". */
    private void setLive(boolean on) {
        if (live == on || web == null) return;
        live = on;
        webLp.flags = on ? catchFlags() : passThroughFlags();
        try { wm.updateViewLayout(web, webLp); } catch (Exception ignored) { }
        web.evaluateJavascript("window.GB&&GB.live(" + on + ")", null);
        bubble.setAlpha(on ? 1f : 0.72f);
        ui.removeCallbacks(release);
        if (on) ui.postDelayed(release, IDLE_RELEASE_MS);
    }

    private class OverlayBridge {
        /** Keeps throw mode open while the user is still throwing. */
        @JavascriptInterface
        public void thrown() { ui.post(new Runnable() {
            @Override public void run() { keepAlive(); }
        }); }
    }

    /** overlay.html calls this on every throw so the mode stays open while busy. */
    void keepAlive() {
        ui.removeCallbacks(release);
        if (live) ui.postDelayed(release, IDLE_RELEASE_MS);
    }

    /** Tap to arm, long-press to wipe, drag to move it out of the way. */
    private class BubbleTouch implements View.OnTouchListener {
        private int dx, dy;
        private float downX, downY;
        private long downAt;
        private boolean moved, wiped;
        private final Runnable longPress = new Runnable() {
            @Override public void run() {
                wiped = true;
                if (web != null) web.evaluateJavascript("window.GB&&GB.wipe()", null);
                bubble.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            }
        };

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dx = bubbleLp.x - (int) e.getRawX();
                    dy = bubbleLp.y - (int) e.getRawY();
                    downX = e.getRawX(); downY = e.getRawY();
                    downAt = System.currentTimeMillis();
                    moved = false; wiped = false;
                    ui.postDelayed(longPress, 550);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getRawX() - downX) > 12 || Math.abs(e.getRawY() - downY) > 12) {
                        moved = true;
                        ui.removeCallbacks(longPress);
                        bubbleLp.x = (int) e.getRawX() + dx;
                        bubbleLp.y = (int) e.getRawY() + dy;
                        try { wm.updateViewLayout(bubble, bubbleLp); } catch (Exception ignored) { }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ui.removeCallbacks(longPress);
                    if (!moved && !wiped && System.currentTimeMillis() - downAt < 550) {
                        setLive(!live);
                    }
                    return true;
            }
            return false;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private Notification note() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "Screen mode", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        Intent stop = new Intent(this, OverlayService.class).setAction(ACTION_STOP);
        int fl = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) fl |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop, fl);

        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return b.setContentTitle("Grudge Booth is on your screen")
                .setContentText("Tap the tomato to throw, long-press it to wipe")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        (android.graphics.drawable.Icon) null, "Stop", stopPi).build())
                .build();
    }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (wm != null) {
            if (web != null) try { wm.removeView(web); } catch (Exception ignored) { }
            if (bubble != null) try { wm.removeView(bubble); } catch (Exception ignored) { }
        }
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
