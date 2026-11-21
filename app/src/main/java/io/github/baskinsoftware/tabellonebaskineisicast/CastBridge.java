package io.github.baskinsoftware.tabellonebaskineisicast;

import android.webkit.JavascriptInterface;

/**
 * Ponte JavaScript ⇄ nativo esposto al WebView di controllo come
 * "window.CastBridge".
 *
 *  - pushState(json): la PWA invia lo stato ad ogni cambio; il nativo aggiorna
 *    il web server integrato (da cui display e TV leggono).
 *  - openRoleSettings(): apre la scelta del ruolo (voce nelle impostazioni PWA).
 *  - showConnectInfo(): mostra indirizzo + QR per collegare un TV via browser.
 *  - quitApp(): chiude l'app nativa. Esposto per un'integrazione esplicita
 *    lato PWA (stesso pattern di openRoleSettings/showConnectInfo, guardia
 *    "typeof cb.quitApp === 'function'" prima di mostrare la voce). Finché
 *    la PWA sorgente non chiama questo bridge, "Chiudi applicazione" resta
 *    affidata a window.close(), gestito lato nativo da
 *    WebChromeClient.onCloseWindow() in MainActivity.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class CastBridge {

    public interface Host {
        void onState(String json);
        void onOpenRoleSettings();
        void onShowConnectInfo();
        void onQuitApp();
    }

    private final Host host;

    public CastBridge(Host host) {
        this.host = host;
    }

    @JavascriptInterface
    public void pushState(String json) {
        if (host != null) host.onState(json);
    }

    @JavascriptInterface
    public void openRoleSettings() {
        if (host != null) host.onOpenRoleSettings();
    }

    @JavascriptInterface
    public void showConnectInfo() {
        if (host != null) host.onShowConnectInfo();
    }

    @JavascriptInterface
    public void quitApp() {
        if (host != null) host.onQuitApp();
    }
}
