package it.uncledan.tabellonebaskineisicast;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import fi.iki.elonen.NanoHTTPD;

/**
 * Piccolo web server integrato per la modalità "sola visualizzazione".
 *
 * Serve i file della PWA (assets/pwa) e un endpoint /state con l'ultimo stato
 * partita in JSON. Il client display (index.html?display=1) fa polling su
 * /state ogni ~0,75 s; il cronometro scorre in locale tra un aggiornamento e
 * l'altro. Lo usano sia il display secondario (WebView interna verso
 * 127.0.0.1) sia i browser dei TV sulla LAN (http://ip-telefono:porta).
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class CastServer extends NanoHTTPD {

    private final Context ctx;
    private volatile String latestState = "{}";

    public CastServer(Context context, int port) {
        super(port);
        this.ctx = context.getApplicationContext();
    }

    /** Aggiorna lo stato corrente (chiamato dal ponte JS del controller). */
    public void setState(String json) {
        if (json != null && !json.isEmpty()) {
            latestState = json;
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        if (uri == null || uri.isEmpty()) {
            uri = "/";
        }

        // Stato corrente in JSON (consumato in polling dal client display)
        if (uri.equals("/state")) {
            Response r = newFixedLengthResponse(Response.Status.OK,
                    "application/json; charset=utf-8", latestState);
            r.addHeader("Cache-Control", "no-store");
            r.addHeader("Access-Control-Allow-Origin", "*");
            return r;
        }

        // Radice: reindirizza alla modalità display (così sul TV basta digitare
        // http://ip-telefono:porta senza ricordare il parametro).
        if (uri.equals("/") || uri.equals("/index.html")) {
            String q = session.getQueryParameterString();
            if (q == null || !q.contains("display=1")) {
                Response r = newFixedLengthResponse(Response.Status.REDIRECT,
                        MIME_HTML, "");
                r.addHeader("Location", "/index.html?display=1");
                return r;
            }
        }

        // File statici dalla PWA in assets/pwa/
        String assetPath = "pwa" + uri;              // uri inizia con "/"
        try {
            AssetManager am = ctx.getAssets();
            InputStream is = am.open(assetPath);
            Response r = newChunkedResponse(Response.Status.OK, mimeOf(uri), is);
            r.addHeader("Cache-Control", "no-store");
            return r;
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "text/plain; charset=utf-8", "Not found");
        }
    }

    private String mimeOf(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".json") || p.endsWith(".webmanifest")) return "application/json; charset=utf-8";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".wav")) return "audio/wav";
        if (p.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
