package it.uncledan.baskintabellonecast;

import android.webkit.JavascriptInterface;

/**
 * Ponte JavaScript ⇄ nativo esposto al WebView di controllo come
 * "window.CastBridge".
 *
 *  - pushState(json): la PWA invia lo stato ad ogni cambio; il nativo aggiorna
 *    il web server integrato (da cui display e TV leggono).
 *  - openRoleSettings(): apre la scelta del ruolo (voce nelle impostazioni PWA).
 *  - showConnectInfo(): mostra indirizzo + QR per collegare un TV via browser.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class CastBridge {

    public interface Host {
        void onState(String json);
        void onOpenRoleSettings();
        void onShowConnectInfo();
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
}
