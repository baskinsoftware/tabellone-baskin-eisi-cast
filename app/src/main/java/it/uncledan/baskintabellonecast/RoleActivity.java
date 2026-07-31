package it.uncledan.baskintabellonecast;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Activity di avvio (LAUNCHER). Ha due compiti:
 *
 *  - al PRIMO avvio (o se richiamata dalle impostazioni con EXTRA_FORCE) mostra
 *    la scelta del ruolo con una preselezione già impostata in base al
 *    dispositivo (TV → "Schermo", altrimenti → "Tabellone"); l'utente conferma
 *    o cambia e prosegue.
 *  - agli avvii successivi instrada direttamente al ruolo memorizzato, senza
 *    mostrare nulla.
 *
 * Autore: Daniele Lolli (UncleDan)
 */
public class RoleActivity extends Activity {

    /** Se true, forza la comparsa della scelta anche se già effettuata. */
    public static final String EXTRA_FORCE = "force_chooser";

    private Roles.Role selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean force = getIntent() != null && getIntent().getBooleanExtra(EXTRA_FORCE, false);
        if (Roles.chosen(this) && !force) {
            route();
            finish();
            return;
        }
        showChooser(force);
    }

    private void showChooser(boolean force) {
        // preselezione: se c'è già una scelta (impostazioni) parto da quella,
        // altrimenti dalla proposta in base al dispositivo
        Roles.Role pre = Roles.chosen(this) ? Roles.stored(this) : Roles.suggested(this);
        selected = pre;

        int pad = dp(24);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(R.string.role_title);
        title.setTextSize(24);
        title.setPadding(0, 0, 0, dp(8));

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.role_subtitle);
        subtitle.setTextSize(15);
        subtitle.setPadding(0, 0, 0, dp(20));

        final RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);

        RadioButton rbMaster  = radio(group, getString(R.string.role_master_label),
                getString(R.string.role_master_desc));
        RadioButton rbDisplay = radio(group, getString(R.string.role_display_label),
                getString(R.string.role_display_desc));
        RadioButton rbAuto    = radio(group, getString(R.string.role_auto_label),
                getString(R.string.role_auto_desc));

        rbMaster.setTag(Roles.Role.MASTER);
        rbDisplay.setTag(Roles.Role.DISPLAY);
        rbAuto.setTag(Roles.Role.AUTO);

        // applica la preselezione
        if (pre == Roles.Role.MASTER) rbMaster.setChecked(true);
        else if (pre == Roles.Role.DISPLAY) rbDisplay.setChecked(true);
        else rbAuto.setChecked(true);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            View v = g.findViewById(checkedId);
            if (v != null && v.getTag() instanceof Roles.Role) {
                selected = (Roles.Role) v.getTag();
            }
        });

        Button cont = new Button(this);
        cont.setText(R.string.role_continue);
        cont.setTextSize(18);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(24);
        cont.setLayoutParams(bp);
        cont.setOnClickListener(v -> {
            Roles.store(this, selected == null ? pre : selected);
            route();
            finish();
        });
        cont.requestFocus();

        root.addView(title);
        root.addView(subtitle);
        root.addView(group);
        root.addView(cont);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        scroll.setBackgroundColor(Color.BLACK);
        setContentView(scroll);
    }

    private RadioButton radio(RadioGroup group, String label, String desc) {
        RadioButton rb = new RadioButton(this);
        rb.setText(label + "\n" + desc);
        rb.setTextSize(17);
        rb.setPadding(dp(8), dp(14), dp(8), dp(14));
        group.addView(rb);
        return rb;
    }

    /** Instrada al ruolo effettivo. */
    private void route() {
        Roles.Role eff = Roles.effective(this);
        Intent i = new Intent(this,
                eff == Roles.Role.DISPLAY ? DisplayActivity.class : MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
