package it.uncledan.baskintabellonecast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.net.InetAddress;

/**
 * Ruolo "schermo": mostra il tabellone in sola visualizzazione su un TV.
 *
 * Trova il tabellone (master) sulla rete tramite NSD (_baskin._tcp) e vi si
 * aggancia da solo caricando la pagina display (…/?display=1) nel WebView a
 * schermo intero. Se la scoperta non va a buon fine, permette di inserire
 * l'indirizzo a mano (fallback). Una pressione prolungata apre la scelta ruolo.
 *
 * Il rendering è la stessa PWA del controller: nessuna grafica duplicata.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class DisplayActivity extends Activity {

    static final String SERVICE_TYPE = "_baskin._tcp.";

    private WebView web;
    private TextView status;
    private final Handler main = new Handler(Looper.getMainLooper());

    private NsdManager nsd;
    private NsdManager.DiscoveryListener discoveryListener;
    private boolean resolving = false;
    private boolean connected = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setText("Ricerca del tabellone sulla rete…");
        status.setTextColor(Color.WHITE);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.addView(status, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        enterImmersive();

        // pressione prolungata → cambia ruolo
        web.setOnLongClickListener(v -> { openRoleSettings(); return true; });
        status.setOnLongClickListener(v -> { openRoleSettings(); return true; });

        nsd = (NsdManager) getSystemService(Context.NSD_SERVICE);

        // se c'è un indirizzo salvato manualmente, prova subito quello
        String manual = Roles.manualHost(this);
        if (!manual.isEmpty()) {
            connectTo(manual);
        }
        startDiscovery();

        // se dopo un po' non si è agganciato nulla, offri l'inserimento manuale
        main.postDelayed(() -> {
            if (!connected) {
                status.setText("Tabellone non trovato automaticamente.\nTieni premuto per opzioni, oppure inserisci l'indirizzo.");
                promptManualHost();
            }
        }, 9000);
    }

    // ------------------------------------------------------------- NSD

    private void startDiscovery() {
        if (nsd == null) return;
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String t, int e) { safeStop(); }
            @Override public void onStopDiscoveryFailed(String t, int e) {}
            @Override public void onDiscoveryStarted(String t) {}
            @Override public void onDiscoveryStopped(String t) {}
            @Override public void onServiceFound(NsdServiceInfo info) {
                String t = info.getServiceType();
                // tollerante al punto finale variabile tra le versioni di Android
                if (t != null && t.contains("_baskin._tcp") && !resolving && !connected) {
                    resolving = true;
                    try { nsd.resolveService(info, resolveListener()); }
                    catch (Exception e) { resolving = false; }
                }
            }
            @Override public void onServiceLost(NsdServiceInfo info) {}
        };
        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception ignored) {}
    }

    private NsdManager.ResolveListener resolveListener() {
        return new NsdManager.ResolveListener() {
            @Override public void onResolveFailed(NsdServiceInfo info, int errorCode) {
                resolving = false;
            }
            @Override public void onServiceResolved(NsdServiceInfo info) {
                resolving = false;
                InetAddress host = info.getHost();
                int port = info.getPort();
                if (host != null && port > 0) {
                    final String hostPort = host.getHostAddress() + ":" + port;
                    main.post(() -> connectTo(hostPort));
                }
            }
        };
    }

    private void safeStop() {
        try { if (nsd != null && discoveryListener != null) nsd.stopServiceDiscovery(discoveryListener); }
        catch (Exception ignored) {}
        discoveryListener = null;
    }

    // ------------------------------------------------------------- Connessione

    private void connectTo(String hostPort) {
        if (connected) return;
        connected = true;
        safeStop();
        status.setVisibility(View.GONE);
        web.loadUrl("http://" + hostPort + "/index.html?display=1");
    }

    private void promptManualHost() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("192.168.1.20:8080");
        String prev = Roles.manualHost(this);
        if (!prev.isEmpty()) input.setText(prev);
        new AlertDialog.Builder(this)
                .setTitle("Indirizzo del tabellone")
                .setMessage("Inserisci IP:porta mostrato dal tabellone.")
                .setView(input)
                .setPositiveButton("Connetti", (d, w) -> {
                    String v = input.getText().toString().trim();
                    if (!v.isEmpty()) {
                        Roles.setManualHost(this, v);
                        connected = false;      // consenti il caricamento manuale
                        connectTo(v);
                    }
                })
                .setNegativeButton("Continua ricerca", null)
                .show();
    }

    private void openRoleSettings() {
        Intent i = new Intent(this, RoleActivity.class);
        i.putExtra(RoleActivity.EXTRA_FORCE, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    // ------------------------------------------------------------- UI

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
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

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        safeStop();
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
