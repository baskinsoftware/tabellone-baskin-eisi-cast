package it.uncledan.baskintabellonecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.app.UiModeManager;
import android.content.pm.PackageManager;

/**
 * Modello dei ruoli del dispositivo e relativa persistenza.
 *
 * L'app è unica e adattiva: allo stesso APK può toccare fare da "tabellone"
 * (controller/server) o da "schermo" (display). Il ruolo è deciso da:
 *  - AUTO   → euristica TV/telefono ad ogni avvio;
 *  - MASTER → forzato controller/server;
 *  - DISPLAY→ forzato schermo.
 *
 * La scelta viene proposta al primo avvio (con preselezione) e resta poi
 * modificabile dalle impostazioni.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public final class Roles {

    public enum Role { AUTO, MASTER, DISPLAY }

    private static final String PREFS = "baskin_cast";
    private static final String K_ROLE = "role";
    private static final String K_CHOSEN = "role_chosen";
    private static final String K_MANUAL_HOST = "manual_host";

    private Roles() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** true dopo che l'utente ha confermato una scelta al primo avvio. */
    public static boolean chosen(Context c) {
        return prefs(c).getBoolean(K_CHOSEN, false);
    }

    /** Ruolo memorizzato (default AUTO). */
    public static Role stored(Context c) {
        String v = prefs(c).getString(K_ROLE, Role.AUTO.name());
        try { return Role.valueOf(v); } catch (Exception e) { return Role.AUTO; }
    }

    /** Salva il ruolo scelto (segna anche la scelta come effettuata). */
    public static void store(Context c, Role r) {
        prefs(c).edit()
                .putString(K_ROLE, r.name())
                .putBoolean(K_CHOSEN, true)
                .apply();
    }

    /** Ruolo effettivo: risolve AUTO in MASTER/DISPLAY con l'euristica. */
    public static Role effective(Context c) {
        Role r = stored(c);
        if (r == Role.AUTO) {
            return isLikelyTv(c) ? Role.DISPLAY : Role.MASTER;
        }
        return r;
    }

    /** Preselezione proposta al primo avvio. */
    public static Role suggested(Context c) {
        return isLikelyTv(c) ? Role.DISPLAY : Role.MASTER;
    }

    /**
     * Euristica "questo dispositivo è una TV / box TV":
     *  - UiModeManager in modalità TELEVISION, oppure
     *  - feature leanback (Android TV), oppure
     *  - assenza di touchscreen.
     * L'override umano ha comunque sempre la precedenza.
     */
    public static boolean isLikelyTv(Context c) {
        try {
            UiModeManager ui = (UiModeManager) c.getSystemService(Context.UI_MODE_SERVICE);
            if (ui != null && ui.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                return true;
            }
        } catch (Exception ignored) {}
        PackageManager pm = c.getPackageManager();
        if (pm != null) {
            if (pm.hasSystemFeature("android.software.leanback")) return true;
            if (!pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)) return true;
        }
        return false;
    }

    /** Indirizzo del tabellone inserito manualmente sul display (fallback NSD). */
    public static String manualHost(Context c) {
        return prefs(c).getString(K_MANUAL_HOST, "");
    }

    public static void setManualHost(Context c, String host) {
        prefs(c).edit().putString(K_MANUAL_HOST, host == null ? "" : host.trim()).apply();
    }
}
