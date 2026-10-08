package com.example.marvelcerto;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.marvelcerto.api.Character;
import java.util.*;

final class GameStore {
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    GameStore(Context context) { prefs = context.getSharedPreferences("marvel_game", Context.MODE_PRIVATE); }
    PlayerProgress progress() {
        try {
            PlayerProgress value = gson.fromJson(prefs.getString("progress", ""), PlayerProgress.class);
            if (value == null) return new PlayerProgress();
            if (value.collection == null) value.collection = new LinkedHashMap<>();
            if (value.questionCounts == null) value.questionCounts = new HashMap<>();
            if (value.credited == null) value.credited = new HashSet<>();
            if (value.history == null) value.history = new ArrayList<>();
            return value;
        } catch (RuntimeException e) { return new PlayerProgress(); }
    }
    QuizGame session() {
        try { return gson.fromJson(prefs.getString("session", ""), QuizGame.class); }
        catch (RuntimeException e) { return null; }
    }
    void save(QuizGame game) { prefs.edit().putString("session", gson.toJson(game)).apply(); }
    QuizGame beginBlock(List<Character> catalog, String category, QuizGame previous) {
        PlayerProgress value = progress();
        QuizGame game = QuizGame.create(catalog, category, new Random(), previous, value.questionCounts);
        for (QuizGame.Question question : game.questions) {
            value.questionCounts.put(question.target.id, value.questionCounts.getOrDefault(question.target.id, 0) + 1);
        }
        prefs.edit().putString("progress", gson.toJson(value)).putString("session", gson.toJson(game)).apply();
        return game;
    }
    void clearSession() { prefs.edit().remove("session").apply(); }
    void credit(QuizGame game) {
        PlayerProgress value = progress();
        if (value.credit(game, System.currentTimeMillis())) prefs.edit().putString("progress", gson.toJson(value)).apply();
    }
    void avatar(Character character) {
        PlayerProgress value = progress(); value.avatar = character;
        prefs.edit().putString("progress", gson.toJson(value)).apply();
    }
    void cache(List<Character> characters) {
        List<Character> marvel = new ArrayList<>();
        if (characters != null) for (Character character : characters) {
            if (character != null && character.biography != null && character.biography.publisher != null
                    && character.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) marvel.add(character);
        }
        prefs.edit().putString("catalog", gson.toJson(marvel)).apply();
    }
    List<Character> catalog() {
        try {
            List<Character> value = gson.fromJson(prefs.getString("catalog", ""), new TypeToken<List<Character>>(){}.getType());
            if (value == null) return new ArrayList<>();
            List<Character> marvel = new ArrayList<>();
            for (Character character : value) if (character != null && character.biography != null
                    && character.biography.publisher != null && character.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) marvel.add(character);
            return marvel;
        } catch (RuntimeException e) { return new ArrayList<>(); }
    }
}
