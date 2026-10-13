package com.ernestomicco.radiofixremover;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Removes the MG4 Radio Fix update from com.saicmotor.service.radio and brings back
 * the factory radio service that is still intact in /system. Same effect as:
 *     adb uninstall com.saicmotor.service.radio
 * Signed with the AOSP platform key, so it gets DELETE_PACKAGES and REBOOT.
 */
public class MainActivity extends Activity {

    static final String RADIO = "com.saicmotor.service.radio";
    static final String ACTION_RESULT = "com.ernestomicco.radiofixremover.RESULT";

    TextView status;
    Button removeBtn, rebootBtn, selfBtn;
    final Handler handler = new Handler();

    final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                // No silent permission: Android shows its own confirmation dialog.
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { startActivity(confirm); } catch (Exception e) { systemDialog(); }
                } else systemDialog();
                return;
            }
            if (st == PackageInstaller.STATUS_SUCCESS) {
                handler.postDelayed(new Runnable() { public void run() { refresh(); } }, 800);
            } else {
                String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                show("Removal failed (" + st + "): " + msg
                        + "\n\nTry the button again, or use Settings > Apps > RadioService > Uninstall updates.");
                removeBtn.setEnabled(true);
            }
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = dp(32);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(18, 18, 18));

        TextView title = new TextView(this);
        title.setText("MG4 Radio Fix Remover");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.rgb(220, 220, 220));
        status.setTextSize(20);
        status.setPadding(0, dp(24), 0, dp(24));
        root.addView(status);

        removeBtn = button("Remove Radio Fix (restore factory radio)");
        removeBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { remove(); } });
        root.addView(removeBtn);

        rebootBtn = button("Restart head unit");
        rebootBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { reboot(); } });
        root.addView(rebootBtn);

        selfBtn = button("Uninstall this helper app");
        selfBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { removeSelf(); } });
        root.addView(selfBtn);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.rgb(18, 18, 18));
        sv.addView(root);
        setContentView(sv);

        IntentFilter f = new IntentFilter(ACTION_RESULT);
        registerReceiver(resultReceiver, f);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    @Override protected void onDestroy() {
        try { unregisterReceiver(resultReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    Button button(String text) {
        Button bt = new Button(this);
        bt.setText(text);
        bt.setAllCaps(false);
        bt.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(88));
        lp.topMargin = dp(16);
        bt.setLayoutParams(lp);
        return bt;
    }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    void show(String s) { status.setText(s); }

    /** Returns: 0 = not found, 1 = factory version only, 2 = an update (the patch) is installed. */
    int radioState(StringBuilder ver) {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(RADIO, 0);
            ver.append(pi.versionName);
            return (pi.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 ? 2 : 1;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    void refresh() {
        StringBuilder v = new StringBuilder();
        int s = radioState(v);
        if (s == 2) {
            show("An update of the radio service is installed:\n" + v
                    + "\n\nTap the red button to remove it. The original factory radio service of your car will come back.");
            removeBtn.setEnabled(true);
        } else if (s == 1) {
            show("Done: the factory radio service is active:\n" + v
                    + "\n\nNow RESTART the head unit (button below, or hold Home for about 20 seconds)."
                    + "\nAfter that you can uninstall this helper app.");
            removeBtn.setEnabled(false);
        } else {
            show("Radio service (" + RADIO + ") not found on this unit. Nothing to do.");
            removeBtn.setEnabled(false);
        }
    }

    void remove() {
        removeBtn.setEnabled(false);
        show("Removing the update...");
        try {
            Intent i = new Intent(ACTION_RESULT).setPackage(getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
            getPackageManager().getPackageInstaller().uninstall(RADIO, pi.getIntentSender());
        } catch (Exception e) {
            systemDialog();
        }
    }

    /** Fallback: Android's standard "Replace this app with the factory version?" dialog. */
    void systemDialog() {
        try {
            Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + RADIO));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            show("Could not remove the update: " + e
                    + "\n\nUse Settings > Apps > RadioService > Uninstall updates.");
            removeBtn.setEnabled(true);
        }
    }

    void reboot() {
        try {
            ((PowerManager) getSystemService(POWER_SERVICE)).reboot(null);
        } catch (Exception e) {
            show(status.getText() + "\n\nAutomatic restart not allowed: hold the Home button for about 20 seconds.");
        }
    }

    void removeSelf() {
        try {
            Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent r = new Intent("com.ernestomicco.radiofixremover.SELF").setPackage(getPackageName());
                PendingIntent pi = PendingIntent.getBroadcast(this, 2, r, 0);
                getPackageManager().getPackageInstaller().uninstall(getPackageName(), pi.getIntentSender());
            } catch (Exception e2) {
                show(status.getText() + "\n\nCould not uninstall this app: " + e2);
            }
        }
    }
}
