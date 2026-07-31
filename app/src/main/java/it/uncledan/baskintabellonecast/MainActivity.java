package it.uncledan.baskintabellonecast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * "Tabellone Baskin EISI (Cast)" — stesse funzionalità del Tabellone Baskin, con in
 * più la presentazione del tabellone su un TV, in sola visualizzazione, via:
 *
 *  1) Display secondario (HDMI / Miracast / "Trasmetti schermo"): un
 *     {@link ScorePresentation} mostra la PWA in modalità display sullo schermo
 *     esterno, mentre il telefono tiene i comandi.
 *  2) LAN: un web server integrato ({@link CastServer}) serve la stessa pagina
 *     di sola visualizzazione; dal browser del TV si apre http://ip-telefono:porta.
 *
 * Il WebView di controllo carica la PWA in modo normale e, ad ogni cambio di
 * stato, la invia al nativo tramite {@link CastBridge}; il nativo aggiorna il
 * server, da cui display secondario e TV leggono in polling.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class MainActivity extends Activity implements CastBridge.Host {

    private static final String APP_HOST = "appassets.androidplatform.net";
    private static final String APP_URL =
            "https://" + APP_HOST + "/assets/pwa/index.html";

    /** Porte tentate in ordine per il web server integrato. */
    private static final int[] PORTS = { 8080, 8081, 8090, 8123, 8888 };

    private WebView web;
    private CastServer server;
    private int serverPort = -1;

    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;
    private ScorePresentation presentation;

    private NsdManager nsd;
    private NsdManager.RegistrationListener nsdRegListener;

    private final Handler main = new Handler(Looper.getMainLooper());

    @SuppressLint({ "SetJavaScriptEnabled", "AddJavascriptInterface" })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // 1) Avvia il web server integrato (prova alcune porte)
        startServer();

        // 2) WebView di controllo (PWA completa, con comandi)
        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        // ponte: la PWA chiama window.CastBridge.* (pushState / apri impostazioni
        // ruolo / mostra indirizzo+QR)
        web.addJavascriptInterface(new CastBridge(this), "CastBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isInternal(uri)) {
                    return false;
                }
                openExternal(uri);
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, Message resultMsg) {
                WebView href = new WebView(view.getContext());
                href.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                        openExternal(req.getUrl());
                        href.destroy();
                        return true;
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(href);
                resultMsg.sendToTarget();
                return true;
            }
        });

        enterImmersive();

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(APP_URL);
        }

        // 3) Display secondario: mostra il tabellone sullo schermo esterno se presente
        setupSecondaryDisplay();

        // 4) Annuncio sulla rete (NSD): i display nativi trovano il tabellone da soli
        registerNsd();

        // mostra come raggiungere il display dalla LAN
        showLanHint();
    }

    // ---------------------------------------------------------- CastBridge.Host

    @Override
    public void onState(String json) {
        if (server != null) server.setState(json);
    }

    @Override
    public void onOpenRoleSettings() {
        main.post(() -> {
            Intent i = new Intent(this, RoleActivity.class);
            i.putExtra(RoleActivity.EXTRA_FORCE, true);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }

    @Override
    public void onShowConnectInfo() {
        main.post(this::showConnectInfoDialog);
    }

    // ------------------------------------------------------------- NSD (master)

    private void registerNsd() {
        if (serverPort < 0) return;
        nsd = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (nsd == null) return;

        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName("Tabellone Baskin");
        info.setServiceType(DisplayActivity.SERVICE_TYPE);
        info.setPort(serverPort);

        nsdRegListener = new NsdManager.RegistrationListener() {
            @Override public void onServiceRegistered(NsdServiceInfo info) {}
            @Override public void onRegistrationFailed(NsdServiceInfo info, int errorCode) {}
            @Override public void onServiceUnregistered(NsdServiceInfo info) {}
            @Override public void onUnregistrationFailed(NsdServiceInfo info, int errorCode) {}
        };
        try {
            nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, nsdRegListener);
        } catch (Exception ignored) {}
    }

    private void unregisterNsd() {
        try { if (nsd != null && nsdRegListener != null) nsd.unregisterService(nsdRegListener); }
        catch (Exception ignored) {}
        nsdRegListener = null;
    }

    // --------------------------------------------------- Dialog indirizzo + QR

    private void showConnectInfoDialog() {
        String ip = localIpAddress();
        if (ip == null || serverPort < 0) {
            toast(getString(R.string.main_no_network));
            return;
        }
        final String url = "http://" + ip + ":" + serverPort + "/?display=1";

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        TextView tv = new TextView(this);
        tv.setText(getString(R.string.main_open_on_tv, url));
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        box.addView(tv);

        int qrPx = Math.round(240 * getResources().getDisplayMetrics().density);
        Bitmap qr = QrUtil.encode(url, qrPx);
        if (qr != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(qr);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(qrPx, qrPx);
            lp.topMargin = pad;
            iv.setLayoutParams(lp);
            iv.setBackgroundColor(Color.WHITE);
            box.addView(iv);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.main_connect_tv_title)
                .setView(box)
                .setPositiveButton(R.string.main_close, null)
                .show();
    }

    // ---------------------------------------------------------------- Server

    private void startServer() {
        for (int port : PORTS) {
            try {
                CastServer srv = new CastServer(this, port);
                srv.start(NanoHTTPD_SOCKET_TIMEOUT, false);
                server = srv;
                serverPort = port;
                return;
            } catch (IOException ignored) {
                // porta occupata: prova la successiva
            }
        }
    }

    // NanoHTTPD.SOCKET_READ_TIMEOUT è protected: usiamo lo stesso valore (10s)
    private static final int NanoHTTPD_SOCKET_TIMEOUT = 10000;

    // ------------------------------------------------------- Display secondario

    private void setupSecondaryDisplay() {
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) return;

        displayListener = new DisplayManager.DisplayListener() {
            @Override public void onDisplayAdded(int displayId) { refreshPresentation(); }
            @Override public void onDisplayRemoved(int displayId) { refreshPresentation(); }
            @Override public void onDisplayChanged(int displayId) { /* no-op */ }
        };
        displayManager.registerDisplayListener(displayListener, main);
        refreshPresentation();
    }

    /** Mostra/aggiorna/chiude la presentazione in base ai display disponibili. */
    private void refreshPresentation() {
        if (displayManager == null || serverPort < 0) return;

        Display[] displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        Display target = (displays != null && displays.length > 0) ? displays[0] : null;

        if (target == null) {
            dismissPresentation();
            return;
        }
        // se stiamo già mostrando sullo stesso display, non fare nulla
        if (presentation != null && presentation.getDisplay() != null
                && presentation.getDisplay().getDisplayId() == target.getDisplayId()) {
            return;
        }
        dismissPresentation();
        try {
            presentation = new ScorePresentation(this, target, serverPort);
            presentation.show();
            toast(getString(R.string.main_external_display));
        } catch (WindowManager.InvalidDisplayException e) {
            presentation = null;
        }
    }

    private void dismissPresentation() {
        if (presentation != null) {
            presentation.dismiss();
            presentation = null;
        }
    }

    // ------------------------------------------------------------- LAN hint

    private void showLanHint() {
        if (serverPort < 0) return;
        String ip = localIpAddress();
        String url = (ip != null)
                ? getString(R.string.main_lan_hint_url, ip, serverPort)
                : getString(R.string.main_lan_hint_port, serverPort);
        // mostrato un po' più a lungo, così si fa in tempo a leggerlo
        main.postDelayed(() -> toast(url), 800);
    }

    private String localIpAddress() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface nif = ifaces.nextElement();
                if (!nif.isUp() || nif.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------- Utility

    private boolean isInternal(Uri uri) {
        return uri != null && APP_HOST.equals(uri.getHost());
    }

    private void openExternal(Uri uri) {
        if (uri == null) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            web.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                  | View.SYSTEM_UI_FLAG_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        enterImmersive();
    }

    @Override
    protected void onDestroy() {
        dismissPresentation();
        unregisterNsd();
        if (displayManager != null && displayListener != null) {
            displayManager.unregisterDisplayListener(displayListener);
        }
        if (server != null) {
            server.stop();
            server = null;
        }
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
