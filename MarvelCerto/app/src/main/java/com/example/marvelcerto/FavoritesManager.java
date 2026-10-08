package com.example.marvelcerto;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "favorites_prefs";
    private static final String FAVORITES_KEY = "favorites_ids";
    private static final String FAVORITES_DATA_KEY = "favorites_data";

    private SharedPreferences prefs;
    private Gson gson;

    public FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public boolean isFavorite(long characterId) {
        Set<String> favoriteIds = prefs.getStringSet(FAVORITES_KEY, new HashSet<>());
        return favoriteIds.contains(String.valueOf(characterId));
    }

    public void addFavorite(com.example.marvelcerto.api.Character character) {
        Set<String> favoriteIds = new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
        favoriteIds.add(String.valueOf(character.id));

        // Salvar dados completos do personagem
        List<com.example.marvelcerto.api.Character> favorites = getAllFavorites();
        favorites.removeIf(c -> c.id == character.id);
        favorites.add(character);
        prefs.edit().putStringSet(FAVORITES_KEY, favoriteIds)
                .putString(FAVORITES_DATA_KEY, gson.toJson(favorites)).apply();
    }

    public void removeFavorite(long characterId) {
        Set<String> favoriteIds = new HashSet<>(prefs.getStringSet(FAVORITES_KEY, new HashSet<>()));
        favoriteIds.remove(String.valueOf(characterId));

        // Remover dos dados completos
        List<com.example.marvelcerto.api.Character> favorites = getAllFavorites();
        favorites.removeIf(c -> c.id == characterId);
        prefs.edit().putStringSet(FAVORITES_KEY, favoriteIds)
                .putString(FAVORITES_DATA_KEY, gson.toJson(favorites)).apply();
    }

    public void toggleFavorite(com.example.marvelcerto.api.Character character) {
        if (isFavorite(character.id)) {
            removeFavorite(character.id);
        } else {
            addFavorite(character);
        }
    }

    public List<com.example.marvelcerto.api.Character> getAllFavorites() {
        String json = prefs.getString(FAVORITES_DATA_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<com.example.marvelcerto.api.Character>>(){}.getType();
        return gson.fromJson(json, type);
    }

}
