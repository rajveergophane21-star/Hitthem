package com.grudgebooth.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Grudge Booth runs entirely inside this WebView from app assets.
 * Nothing the player picks ever leaves the device.
 */
public class MainActivity extends AppCompatActivity {

    private WebView web;
    private ValueCallback<Uri[]> pendingFiles;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    /** Hands the picked image back to the page's <input type="file">. */
    private final ActivityResultLauncher<Intent> picker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (pendingFiles == null) return;
                        Uri[] picked = null;
                        Intent data = result.getData();
                        if (result.getResultCode() == RESULT_OK && data != null) {
                            if (data.getClipData() != null) {
                                int n = data.getClipData().getItemCount();
                                picked = new Uri[n];
                                for (int i = 0; i < n; i++) {
                                    picked[i] = data.getClipData().getItemAt(i).getUri();
                                }
                            } else if (data.getData() != null) {
                                picked = new Uri[]{ data.getData() };
                            }
                        }
                        pendingFiles.onReceiveValue(picked);
                        pendingFiles = null;
                    });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0F0E0D);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebViewClient(new WebViewClient() {
            /* A JS bridge is only as safe as the page that can reach it, so keep
               the WebView on the asset we ship and let nothing navigate away. */
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                return !("file".equals(u.getScheme())
                        && String.valueOf(u.getPath()).startsWith("/android_asset/"));
            }
        });
        web.addJavascriptInterface(new Host(), "GrudgeHost");

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                                             ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try {
                    picker.launch(params.createIntent());
                } catch (Exception e) {
                    pendingFiles = null;
                    return false;
                }
                return true;
            }
        });

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        goImmersive();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (web.canGoBack()) web.goBack(); else finish();
            }
        });
    }

    /**
     * Measures the subject so the game can size a splat against the face instead
     * of against the frame. The page hands over a downscaled JPEG; we hand back
     * a face width in that image's pixels, or 0 when nothing was found - in
     * which case the game just keeps its default size.
     */
    private class Host {
        @JavascriptInterface
        public void measureFace(final String jpegBase64) {
            io.execute(new Runnable() {
                @Override public void run() { report(widthOfLargestFace(jpegBase64)); }
            });
        }
    }

    @SuppressWarnings("deprecation") // android.media.FaceDetector: old, but on-device and dependency-free
    private float widthOfLargestFace(String jpegBase64) {
        Bitmap bmp = null;
        try {
            byte[] raw = Base64.decode(jpegBase64, Base64.DEFAULT);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565; // FaceDetector accepts nothing else
            bmp = BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
            if (bmp == null) return 0f;
            if (bmp.getConfig() != Bitmap.Config.RGB_565) {
                Bitmap c = bmp.copy(Bitmap.Config.RGB_565, false);
                bmp.recycle();
                bmp = c;
                if (bmp == null) return 0f;
            }
            int w = bmp.getWidth() & ~1; // findFaces rejects an odd width
            int h = bmp.getHeight();
            if (w < 2 || h < 2) return 0f;
            if (w != bmp.getWidth()) {
                Bitmap c = Bitmap.createBitmap(bmp, 0, 0, w, h);
                bmp.recycle();
                bmp = c;
                if (bmp == null) return 0f;
            }

            int max = 8;
            FaceDetector.Face[] found = new FaceDetector.Face[max];
            int n = new FaceDetector(w, h, max).findFaces(bmp, found);

            float best = 0f;
            for (int i = 0; i < n; i++) {
                FaceDetector.Face f = found[i];
                if (f == null || f.confidence() < 0.3f) continue;
                // eye separation is the one solid measurement it gives us;
                // cheekbone width runs about 2.2x that on an adult face.
                float width = f.eyesDistance() * 2.2f;
                if (width > best) best = width;
            }
            return best;
        } catch (Throwable t) {
            return 0f; // a miss is not worth crashing a game over
        } finally {
            if (bmp != null && !bmp.isRecycled()) bmp.recycle();
        }
    }

    private void report(final float faceWidth) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (web == null) return;
                web.evaluateJavascript(
                        "window.__grudgeFace&&window.__grudgeFace(" + faceWidth + ")", null);
            }
        });
    }

    private void goImmersive() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), web);
        c.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        c.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) goImmersive();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    @Override protected void onPause()  { super.onPause();  if (web != null) web.onPause();  }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
}
