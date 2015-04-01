package org.fdroid.fdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;
import org.fdroid.fdroid.compat.Compatibility;
import org.fdroid.fdroid.compat.SupportedArchitectures;
import org.fdroid.fdroid.data.Apk;

import java.util.*;

// Call getIncompatibleReasons(apk) on an instance of this class to
    // find reasons why an apk may be incompatible with the user's device.
public class CompatibilityChecker extends Compatibility {

    private static final String TAG = "fdroid.Compatibility";

    private Context context;
    private Set<String> features;
    private String[] cpuAbis;
    private String cpuAbisDesc;
    private boolean ignoreTouchscreen;

    public CompatibilityChecker(Context ctx) {

        context = ctx.getApplicationContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        ignoreTouchscreen = prefs.getBoolean(Preferences.PREF_IGN_TOUCH, false);

        PackageManager pm = ctx.getPackageManager();
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("Available device features:");
        features = new HashSet<>();
        if (pm != null) {
            final FeatureInfo[] featureArray = pm.getSystemAvailableFeatures();
            if (featureArray != null)
                for (FeatureInfo fi : pm.getSystemAvailableFeatures()) {
                    features.add(fi.name);
                    logMsg.append('\n');
                    logMsg.append(fi.name);
                }
        }

        cpuAbis = SupportedArchitectures.getAbis();

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (final String abi : cpuAbis) {
            if (first)
                first = false;
            else
                builder.append(", ");
            builder.append(abi);
        }
        cpuAbisDesc = builder.toString();

        Log.d(TAG, logMsg.toString());
    }

    private boolean compatibleApi(Utils.CommaSeparatedList nativecode) {
        if (nativecode == null) return true;
        for (final String abi : nativecode) {
            if (Utils.arrayContains(cpuAbis, abi)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getIncompatibleReasons(final Apk apk) {

        List<String> incompatibleReasons = new ArrayList<>();

        if (!hasApi(apk.minSdkVersion) || !upToApi(apk.maxSdkVersion)) {
            incompatibleReasons.add(
                context.getResources().getString(
                    R.string.minsdk_or_later,
                    Utils.getAndroidVersionName(apk.minSdkVersion)));
        }

        if (apk.features != null) {
            for (final String feat : apk.features) {
                if (ignoreTouchscreen
                        && feat.equals("android.hardware.touchscreen")) {
                    // Don't check it!
                } else if (!features.contains(feat)) {
                    Collections.addAll(incompatibleReasons, feat.split(","));
                    Log.d(TAG, apk.id + " vercode " + apk.vercode
                            + " is incompatible based on lack of "
                            + feat);
                }
            }
        }
        if (!compatibleApi(apk.nativecode)) {
            for (final String code : apk.nativecode) {
                incompatibleReasons.add(code);
            }
            Log.d(TAG, apk.id + " vercode " + apk.vercode
                    + " only supports " + Utils.CommaSeparatedList.str(apk.nativecode)
                    + " while your architectures are " + cpuAbisDesc);
        }

        return incompatibleReasons;
    }
}
