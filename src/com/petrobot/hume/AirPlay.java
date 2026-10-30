package com.petrobot.hume;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** Explicit integration with the separate receiver; no privileged broadcasts or background repair. */
final class AirPlay {
    static final String PKG = "io.github.jqssun.airplay";
    static final String DOWNLOAD = "https://github.com/jqssun/android-airplay-server/releases";

    private AirPlay() { }

    static Intent launchIntent(Context c) {
        Intent intent = c.getPackageManager().getLeanbackLaunchIntentForPackage(PKG);
        return intent != null ? intent : c.getPackageManager().getLaunchIntentForPackage(PKG);
    }

    static boolean installed(Context c) {
        return launchIntent(c) != null;
    }

    /** Receiver startup, background operation and permission prompts belong to its own UI. */
    static void open(Activity activity) {
        Intent intent = launchIntent(activity);
        if (intent == null) {
            new AlertDialog.Builder(activity)
                    .setTitle("Set up AirPlay")
                    .setMessage("Install the separate AirPlay Server app from:\n" + DOWNLOAD
                            + "\n\nThen open it to approve its permissions and configure startup."
                            + " Hume does not install apps automatically.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "AirPlay receiver is unavailable. Open its app settings and try again.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
