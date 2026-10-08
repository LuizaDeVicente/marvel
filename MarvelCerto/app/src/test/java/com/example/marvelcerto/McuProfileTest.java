package com.example.marvelcerto;

import org.junit.Test;
import static org.junit.Assert.*;

public class McuProfileTest {
    @Test public void ironManHasMockupMoviesAndEnemyAliases() {
        McuProfile profile = McuProfile.forName("Iron Man");
        assertEquals(5, profile.movies.size());
        assertEquals(2008, profile.movies.get(0).year);
        assertEquals("Obadiah Stane", profile.opponents.get(1).name);
        assertEquals("Iron Monger", profile.opponents.get(1).apiName);
        assertEquals("Homem de Ferro", profile.opponents.get(1).movies);
    }
    @Test public void villainHasHeroesAndDoesNotInheritHeroMovies() {
        McuProfile profile = McuProfile.forName("thanos");
        assertEquals(6, profile.opponents.size());
        assertEquals("Iron Man", profile.opponents.get(0).apiName);
        assertEquals(2018, profile.movies.get(0).year);
    }
    @Test public void unknownCharactersDoNotReceiveInventedData() {
        assertTrue(McuProfile.forName("Unknown").movies.isEmpty());
        assertTrue(McuProfile.forName(null).opponents.isEmpty());
    }
    @Test public void villainsHaveReverseConfrontationsWithMovieContext() {
        for (McuProfile.Opponent enemy : McuProfile.forName("Iron Man").opponents) {
            McuProfile villain = McuProfile.forName(enemy.apiName);
            boolean found = false;
            for (McuProfile.Opponent hero : villain.opponents) {
                if ("Iron Man".equals(hero.apiName)) {
                    assertEquals(enemy.movies, hero.movies);
                    found = true;
                }
            }
            assertTrue(enemy.apiName, found);
        }
    }
    @Test public void villainAliasesResolveToTheSameHero() {
        for (String alias : new String[]{"Iron Monger", "Obadiah Stane", "Whiplash", "Ivan Vanko", "Ultron", "Mandarin"}) {
            assertEquals("Iron Man", McuProfile.forName(alias).opponents.get(0).apiName);
        }
    }
}
