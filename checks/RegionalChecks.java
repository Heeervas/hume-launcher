package com.petrobot.hume;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Runs actual request producers, preferences migrations and RSS parser without an Android emulator. */
public final class RegionalChecks {
    private RegionalChecks() { }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    static void requests() throws Exception {
        String title = "El ni\u00f1o \"especial\"";
        JSONObject request = Search.request(title);
        JSONObject vars = request.getJSONObject("variables");
        require(vars.getString("country").equals("ES"), "Search must request Spain availability");
        require(vars.getString("language").equals("es"), "Search metadata must be Spanish");
        require(vars.getString("q").equals(title), "Quoted accented query must survive JSON serialization");
        require(request.getString("query").contains("offers(country: $country"), "Offers must share country variable");
        for (Suggestions.Provider provider : Suggestions.PROVIDERS) {
            JSONObject rec = Suggestions.justWatchRequest(provider, "tv").getJSONObject("variables");
            require(rec.getString("country").equals("ES"), "Recommendation region must match Search");
            require(rec.getJSONObject("filter").getJSONArray("objectTypes").getString(0).equals("SHOW"), "TV kind");
            require(!provider.jwTech.contains("hotstar") && !provider.jwTech.equals("zee5"), "No India-only service");
        }
        String tmdb = Suggestions.tmdbPath("movie", 8, set());
        require(tmdb.contains("watch_region=ES") && tmdb.contains("language=es-ES"), "TMDB Spain metadata");
        require(!tmdb.contains("with_original_language"), "Spanish metadata must not restrict original language");
        require(Suggestions.tmdbPath("tv", 8, set("en")).contains("with_original_language=en"), "Explicit language retained");
        require(SearchPanel.SPANISH.contains("\u00f1") && SearchPanel.SPANISH.contains("\u00e1"), "Spanish character input");
    }

    static void selections() {
        MemoryPreferences old = new MemoryPreferences();
        old.values.put("providers", set("2336", "232"));
        old.values.put("languages", set("hi", "ta"));
        old.values.put("news_sources", set("toi"));
        old.values.put("tmdb_token", "fixture-token");
        old.values.put("shelf", "none");
        Regional.migrateSelections(old.prefs);
        require(old.prefs.getStringSet("providers", set()).contains("8"), "Removed-only providers need Spain defaults");
        require(old.prefs.getStringSet("languages", set()).isEmpty(), "Removed-only languages become unrestricted");
        require(old.prefs.getStringSet("news_sources", set()).equals(Regional.NEWS), "Indian newsroom IDs must migrate");
        require(old.prefs.getString("tmdb_token", "").equals("fixture-token") && old.prefs.getString("shelf", "").equals("none"), "Unrelated preferences preserved");
        old.prefs.edit().putStringSet("providers", set("119")).apply();
        Regional.migrateSelections(old.prefs);
        require(old.prefs.getStringSet("providers", set()).equals(set("119")), "Migration is idempotent");
        MemoryPreferences mixed = new MemoryPreferences();
        mixed.values.put("providers", set("8", "2336"));
        mixed.values.put("languages", set("en", "hi"));
        mixed.values.put("news_sources", set());
        Regional.migrateSelections(mixed.prefs);
        require(mixed.prefs.getStringSet("providers", set()).equals(set("8")), "Retained providers must not gain extras");
        require(mixed.prefs.getStringSet("languages", set()).equals(set("en")), "Retained language preserved");
        require(mixed.prefs.getStringSet("news_sources", set()).isEmpty(), "Explicit empty selection preserved");
    }

    static void weather() {
        MemoryPreferences fresh = new MemoryPreferences();
        fresh.values.put("cache2", "old automatic forecast");
        fresh.values.put("place", "Old automatic city");
        fresh.values.put("enabled", false);
        Regional.seedWeather(fresh.prefs);
        require(fresh.prefs.getString("place", "").equals("Madrid"), "Unconfigured weather defaults to Madrid");
        require(Math.abs(fresh.prefs.getFloat("lat", 0) - 40.4168f) < 0.0001, "Madrid latitude");
        require(!fresh.prefs.contains("cache2"), "Old location forecast must be invalidated");
        require(!fresh.prefs.getBoolean("enabled", true), "Disabled weather must remain disabled");
        MemoryPreferences automatic = new MemoryPreferences();
        automatic.values.put("manual", false);
        automatic.values.put("place", "Automatic city");
        Regional.seedWeather(automatic.prefs);
        require(!automatic.prefs.getBoolean("manual", true), "Explicit automatic choice preserved");
        require(automatic.prefs.getString("place", "").equals("Automatic city"), "Automatic city preserved");
        MemoryPreferences manual = new MemoryPreferences();
        manual.values.put("manual", true);
        manual.values.put("place", "Barcelona");
        manual.values.put("units", "f");
        Regional.seedWeather(manual.prefs);
        require(manual.prefs.getString("place", "").equals("Barcelona") && manual.prefs.getString("units", "").equals("f"), "Manual city and units preserved");
    }

    static void feedFixture() throws Exception {
        String date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(new java.util.Date());
        String xml = "<rss xmlns:media=\"http://search.yahoo.com/mrss/\"><channel>"
                + "<item><title>Madrid &amp; cultura</title><description><![CDATA[<b>Resumen</b>]]></description>"
                + "<pubDate>" + date + "</pubDate><media:content url=\"https://example.test/image.jpg\" type=\"image/jpeg\"/></item>"
                + "<item><title>No image</title></item>"
                + "<item><title>Stale</title><pubDate>Wed, 01 Jan 2020 00:00:00 +0000</pubDate>"
                + "<enclosure url=\"https://example.test/old.jpg\" type=\"image/jpeg\"/></item></channel></rss>";
        List<Suggestions.Item> items = News.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), "Madrid", 86400000L);
        require(items.size() == 1, "RSS must reject stale and imageless cards");
        require(items.get(0).title.equals("Madrid & cultura") && items.get(0).overview.equals("Resumen"), "RSS entities and HTML");
        require(items.get(0).genres.equals("Madrid"), "Local feed label must be Madrid");
        boolean failed = false;
        try {
            News.parse(new ByteArrayInputStream("<rss><item></rss>".getBytes(StandardCharsets.UTF_8)), "Madrid", 86400000L);
        } catch (org.xmlpull.v1.XmlPullParserException expected) {
            failed = true;
        }
        require(failed, "Malformed feed must fail rather than invent cards");
    }

    static void live() throws Exception {
        JSONObject response = Search.post("https://apis.justwatch.com/graphql", Search.request("La casa de papel").toString());
        require(!response.has("errors"), "JustWatch search must accept actual request: " + response.opt("errors"));
        require(response.getJSONObject("data").getJSONObject("popularTitles").getJSONArray("edges").length() > 0, "Spain live search results");
        for (Suggestions.Provider provider : Suggestions.PROVIDERS) {
            JSONObject rec = Suggestions.post("https://apis.justwatch.com/graphql", Suggestions.justWatchRequest(provider, "movie").toString());
            require(!rec.has("errors"), "Provider request rejected: " + provider.name);
            JSONArray edges = rec.getJSONObject("data").getJSONObject("popularTitles").getJSONArray("edges");
            require(edges.length() > 0, "Provider must return actual titles: " + provider.name);
            int matchingOffers = 0;
            for (int i = 0; i < edges.length(); i++) {
                JSONArray offers = edges.getJSONObject(i).getJSONObject("node").optJSONArray("offers");
                for (int j = 0; offers != null && j < offers.length(); j++) {
                    if (provider.jwTech.equals(offers.getJSONObject(j).getJSONObject("package").optString("technicalName"))) {
                        matchingOffers++;
                    }
                }
            }
            require(matchingOffers > 0, "Provider must return matching service offers: " + provider.name);
            System.out.println("Spain provider accepted: " + provider.name + ", titles=" + edges.length() + ", matchingOffers=" + matchingOffers);
        }
        for (News.Source source : News.SOURCES) {
            for (String url : new String[]{source.national, source.local, source.world}) {
                if (url == null) continue;
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(8000);
                con.setReadTimeout(12000);
                con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android TV) HumeLauncher");
                try (InputStream in = con.getInputStream()) {
                    require(!News.parse(in, source.name, 7L * 86400000).isEmpty(), "Feed must yield image-bearing cards: " + url);
                    System.out.println("RSS parser accepted: " + url);
                } finally {
                    con.disconnect();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        requests();
        selections();
        weather();
        feedFixture();
        if (args.length > 0 && args[0].equals("live")) live();
        System.out.println("Regional request, migration, weather and RSS checks passed");
    }
}
