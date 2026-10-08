package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterFilterTest {
    private Character member(String teams) {
        Character character = new Character();
        character.connections = new Character.Connections();
        character.connections.groupAffiliation = teams;
        return character;
    }

    @Test public void filtersTeamsAcrossMultipleAffiliationsAndCase() {
        Character avenger = member("S.H.I.E.L.D.; Avengers; New Avengers");
        Character guardian = member("GUARDIANS OF THE GALAXY; Infinity Watch");
        assertTrue(CharacterFilter.AVENGERS.matches(avenger));
        assertFalse(CharacterFilter.GUARDIANS.matches(avenger));
        assertTrue(CharacterFilter.GUARDIANS.matches(guardian));
        assertFalse(CharacterFilter.AVENGERS.matches(guardian));
        assertTrue(CharacterFilter.X_MEN.matches(member("X-Men; X-Force")));
    }

    @Test public void allRestoresCharactersWithoutAffiliations() {
        Character noTeams = new Character();
        assertTrue(CharacterFilter.ALL.matches(noTeams));
        assertFalse(CharacterFilter.AVENGERS.matches(noTeams));
        assertFalse(CharacterFilter.GUARDIANS.matches(member(null)));
        assertFalse(CharacterFilter.GUARDIANS.matches(member("Guardians of the Multiverse")));
    }

    @Test public void villainsUseAlignmentIndependentlyOfTeam() {
        Character villain = member(null);
        villain.biography = new Character.Biography();
        villain.biography.alignment = "bad";
        assertTrue(CharacterFilter.VILLAINS.matches(villain));
        villain.biography.alignment = "good";
        assertFalse(CharacterFilter.VILLAINS.matches(villain));
    }
}
