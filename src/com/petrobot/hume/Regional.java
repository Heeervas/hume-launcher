package com.petrobot.hume;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Spain content defaults and a one-time migration; never resets profiles or credentials. */
final class Regional {
    static final String COUNTRY = "ES", LANGUAGE = "es", LOCALE = "es-ES";
    static final String CITY = "Madrid", CONTENT = "es_v1";
    static final float LATITUDE = 40.4168f, LONGITUDE = -3.7038f;
    static final Set<String> NEWS = new HashSet<>(Arrays.asList("elpais", "20minutos"));
    private static final String MIGRATION = "spain_content_version";

    private Regional() { }

    static void migrateSelections(SharedPreferences p) {
        if (p.getInt(MIGRATION, 0) >= 1) return;
        Set<String> providers = new HashSet<>();
        for (Suggestions.Provider provider : Suggestions.PROVIDERS) providers.add(String.valueOf(provider.id));
        SharedPreferences.Editor edit = p.edit();
        migrateSet(p, edit, Suggestions.K_PROVIDERS, providers, true);
        migrateSet(p, edit, Suggestions.K_LANGS, new HashSet<>(Arrays.asList(Suggestions.LANG_CODES)), false);
        migrateSet(p, edit, "news_sources", NEWS, true);
        edit.putInt(MIGRATION, 1).apply();
    }

    /** An explicit empty set is intentional; removed-only language filters become unrestricted. */
    private static void migrateSet(SharedPreferences p, SharedPreferences.Editor edit,
                                   String key, Set<String> valid, boolean fallback) {
        if (!p.contains(key)) return;
        Set<String> before = new HashSet<>(p.getStringSet(key, new HashSet<>()));
        Set<String> after = new HashSet<>(before);
        after.retainAll(valid);
        if (fallback && !before.isEmpty() && after.isEmpty()) after.addAll(valid);
        edit.putStringSet(key, after);
    }

    /** Only absent location choice means unconfigured; false explicitly means automatic location. */
    static void seedWeather(SharedPreferences p) {
        if (p.contains(Weather.K_MANUAL)) return;
        SharedPreferences.Editor edit = p.edit().putBoolean(Weather.K_MANUAL, true)
                .putString(Weather.K_PLACE, CITY).putFloat(Weather.K_LAT, LATITUDE).putFloat(Weather.K_LON, LONGITUDE)
                .remove(Weather.K_CACHE).remove(Weather.K_CACHE_AT).remove(Weather.K_CACHE_KEY);
        if (!p.contains(Weather.K_UNITS)) edit.putString(Weather.K_UNITS, "c");
        edit.apply();
    }
}
