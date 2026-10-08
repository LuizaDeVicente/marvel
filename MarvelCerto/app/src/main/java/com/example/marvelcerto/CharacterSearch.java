package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CharacterSearch {
    static List<Character> find(List<Character> characters, String query) {
        List<Character> result = new ArrayList<>();
        String term = normalize(query);
        if (term.isEmpty()) return result;
        for (Character character : characters) {
            if (character != null && character.biography != null
                    && normalize(character.biography.publisher).contains("marvel")
                    && (normalize(character.name).contains(term)
                    || normalize(character.biography.fullName).contains(term))) result.add(character);
        }
        return result;
    }

    private static String normalize(String text) {
        return text == null ? "" : Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
