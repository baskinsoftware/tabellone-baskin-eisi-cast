package it.uncledan.tabellonebaskineisicast;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Contenuto mostrato sul display secondario (HDMI / Miracast / "Trasmetti
 * schermo"): un WebView che carica la PWA in modalità sola visualizzazione
 * dal web server integrato (127.0.0.1), quindi senza alcun comando.
 *
 * Riceve gli aggiornamenti in polling da /state, esattamente come farebbe il
 * browser di un TV sulla LAN: un unico percorso per entrambi i trasporti.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class ScorePresentation extends Presentation {

    private final int port;
    private WebView web;

    public ScorePresentation(Context outerContext, Display display, int port) {
        super(outerContext, display);
        this.port = port;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(getContext());
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        setContentView(web);
        web.loadUrl("http://127.0.0.1:" + port + "/index.html?display=1");
    }

    @Override
    public void onDetachedFromWindow() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDetachedFromWindow();
    }
}
