package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import java.util.Locale;

/** Team membership follows the affiliations supplied by the character API. */
public enum CharacterFilter {
    ALL, AVENGERS, GUARDIANS, X_MEN, VILLAINS;

    public boolean matches(Character character) {
        if (character == null) return false;
        if (this == ALL) return true;
        if (this == VILLAINS) return character.isVillain();
        String affiliation = character.connections == null ? null : character.connections.groupAffiliation;
        if (affiliation == null) return false;
        String teams = affiliation.toLowerCase(Locale.ROOT);
        switch (this) {
            case AVENGERS: return teams.contains("avengers");
            case GUARDIANS: return teams.contains("guardians of the galaxy");
            case X_MEN: return teams.contains("x-men") || teams.contains("x men");
            default: return false;
        }
    }
}
