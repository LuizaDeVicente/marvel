package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class CharacterSearchTest {
    private Character character(String name, String realName, String publisher) {
        Character c = new Character();
        c.name = name;
        c.biography = new Character.Biography();
        c.biography.fullName = realName;
        c.biography.publisher = publisher;
        return c;
    }

    @Test public void acceptsHomeQueryIgnoringCaseWhitespaceAndAccents() {
        Character hero = character("Iron Man", "Antônio Stark", "Marvel Comics");
        List<Character> source = Arrays.asList(hero);
        assertEquals(source, CharacterSearch.find(source, "  IRON MAN "));
        assertEquals(source, CharacterSearch.find(source, "antonio"));
    }

    @Test public void clearingOrChangingQueryDoesNotKeepOldResults() {
        Character iron = character("Iron Man", "Tony Stark", "Marvel Comics");
        Character thor = character("Thor", "Thor Odinson", "Marvel Comics");
        List<Character> source = Arrays.asList(iron, thor);
        assertEquals(Arrays.asList(iron), CharacterSearch.find(source, "iron"));
        assertEquals(Arrays.asList(thor), CharacterSearch.find(source, "thor"));
        assertTrue(CharacterSearch.find(source, "  ").isEmpty());
        assertTrue(CharacterSearch.find(source, "not found").isEmpty());
        assertEquals(2, source.size());
    }

    @Test public void handlesMissingFieldsAndExcludesOtherPublishers() {
        List<Character> source = Arrays.asList(null, new Character(),
                character(null, null, "Marvel Comics"), character("Batman", "Bruce Wayne", "DC Comics"));
        assertTrue(CharacterSearch.find(source, "batman").isEmpty());
        assertTrue(CharacterSearch.find(source, null).isEmpty());
    }
}
