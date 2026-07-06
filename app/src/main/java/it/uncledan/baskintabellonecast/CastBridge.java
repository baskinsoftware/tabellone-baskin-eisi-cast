package it.uncledan.baskintabellonecast;

import android.webkit.JavascriptInterface;

/**
 * Ponte JavaScript ⇄ nativo esposto al WebView di controllo come
 * "window.CastBridge". La PWA, ad ogni cambio di stato, chiama
 * CastBridge.pushState(json); il nativo aggiorna il web server integrato,
 * da cui il display secondario e i browser dei TV leggono in polling.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class CastBridge {

    public interface Listener {
        void onState(String json);
    }

    private final Listener listener;

    public CastBridge(Listener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void pushState(String json) {
        if (listener != null) {
            listener.onState(json);
        }
    }
}
