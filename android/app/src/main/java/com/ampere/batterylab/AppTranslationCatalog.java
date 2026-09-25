package com.ampere.batterylab;

import android.content.Context;
import android.util.LruCache;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline translations for languages beyond the German and English source layer. */
final class AppTranslationCatalog {
    private static final String ASSET_PATH = "locales/app-translations.json";
    private static final String[] LANGUAGES = {"es", "fr", "it", "pt-BR", "nl"};
    private static final LruCache<String, String> TRANSLATION_CACHE = new LruCache<>(512);
    private static volatile Map<String, List<String[]>> translationsByLanguage;

    private AppTranslationCatalog() { }

    static String translate(Context context, String languageTag, String value) {
        if (context == null || value == null || value.isEmpty()) return value;
        String cacheKey = languageTag + '\u0000' + value;
        String cached = TRANSLATION_CACHE.get(cacheKey);
        if (cached != null) return cached;
        List<String[]> phrases = translations(context, languageTag);
        if (phrases.isEmpty()) return value;

        String result = value;
        ArrayList<String> protectedResults = new ArrayList<>();
        for (String[] phrase : phrases) {
            if (phrase[0].isEmpty() || !result.contains(phrase[0])) continue;
            String token = "\uE100" + protectedResults.size() + "\uE101";
            String replaced = replacePhrase(result, phrase[0], token);
            if (replaced.equals(result)) continue;
            result = replaced;
            protectedResults.add(phrase[1]);
        }
        for (int i = 0; i < protectedResults.size(); i++) {
            result = result.replace("\uE100" + i + "\uE101", protectedResults.get(i));
        }
        result = preserveAkkuTaktBrand(value, result);
        TRANSLATION_CACHE.put(cacheKey, result);
        return result;
    }

    private static String preserveAkkuTaktBrand(String source, String translated) {
        if (!source.contains("AkkuTakt")) return translated;
        return translated.replace("Akkutakt", "AkkuTakt")
                .replace("Akku Takt", "AkkuTakt");
    }

    private static String replacePhrase(String value, String source, String replacement) {
        if (!source.matches("[\\p{L}\\p{N}_]+")) return value.replace(source, replacement);
        Pattern wholeWord = Pattern.compile("(?<![\\p{L}\\p{N}_])"
                + Pattern.quote(source) + "(?![\\p{L}\\p{N}_])");
        Matcher matcher = wholeWord.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static List<String[]> translations(Context context, String languageTag) {
        Map<String, List<String[]>> cached = translationsByLanguage;
        if (cached == null) {
            synchronized (AppTranslationCatalog.class) {
                cached = translationsByLanguage;
                if (cached == null) {
                    cached = load(context);
                    translationsByLanguage = cached;
                }
            }
        }
        List<String[]> phrases = cached.get(languageTag);
        return phrases == null ? Collections.<String[]>emptyList() : phrases;
    }

    private static Map<String, List<String[]>> load(Context context) {
        HashMap<String, List<String[]>> result = new HashMap<>();
        StringBuilder json = new StringBuilder();
        try (InputStream stream = context.getAssets().open(ASSET_PATH);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
        } catch (IOException ignored) {
            return result;
        }

        try {
            JSONObject catalog = new JSONObject(json.toString());
            for (String language : LANGUAGES) {
                JSONObject entries = catalog.optJSONObject(language);
                if (entries == null) continue;
                ArrayList<String[]> phrases = new ArrayList<>();
                for (java.util.Iterator<String> keys = entries.keys(); keys.hasNext();) {
                    String english = keys.next();
                    String localized = entries.optString(english, "");
                    if (!english.isEmpty() && !localized.isEmpty() && !english.equals(localized)) {
                        phrases.add(new String[]{english, localized});
                    }
                }
                Collections.sort(phrases, new Comparator<String[]>() {
                    @Override public int compare(String[] left, String[] right) {
                        return Integer.compare(right[0].length(), left[0].length());
                    }
                });
                result.put(language, phrases);
            }
        } catch (JSONException ignored) {
            return new HashMap<>();
        }
        return result;
    }
}
