package com.example.marvelcerto;

import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/** Curated entries from the supplied MarvelVerse mockup, not a complete MCU database. */
final class McuProfile {
    static final class Movie {
        final String title;
        final int year;
        Movie(String title, int year) { this.title = title; this.year = year; }
    }
    static final class Opponent {
        final String name;
        final String apiName;
        final String movies;
        Opponent(String name, String apiName, String movies) {
            this.name = name; this.apiName = apiName; this.movies = movies;
        }
    }
    final List<Movie> movies;
    final List<Opponent> opponents;
    private McuProfile(List<Movie> movies, List<Opponent> opponents) {
        this.movies = Collections.unmodifiableList(movies);
        this.opponents = Collections.unmodifiableList(opponents);
    }
    static McuProfile forName(String name) {
        if ("Iron Man".equalsIgnoreCase(name)) {
            return new McuProfile(Arrays.asList(
                    new Movie("Homem de Ferro", 2008), new Movie("Homem de Ferro 2", 2010),
                    new Movie("Os Vingadores", 2012), new Movie("Homem de Ferro 3", 2013),
                    new Movie("Vingadores: Era de Ultron", 2015)), Arrays.asList(
                    new Opponent("Thanos", "Thanos", "Vingadores: Guerra Infinita · Ultimato"),
                    new Opponent("Obadiah Stane", "Iron Monger", "Homem de Ferro"),
                    new Opponent("Ivan Vanko", "Whiplash", "Homem de Ferro 2"),
                    new Opponent("Mandarin", "Mandarin", "Homem de Ferro 3"),
                    new Opponent("Ultron", "Ultron", "Vingadores: Era de Ultron")));
        }
        if ("Thanos".equalsIgnoreCase(name)) {
            return new McuProfile(Arrays.asList(new Movie("Vingadores: Guerra Infinita", 2018),
                    new Movie("Vingadores: Ultimato", 2019)), Arrays.asList(
                    new Opponent("Iron Man", "Iron Man", "Vingadores: Guerra Infinita · Ultimato"),
                    new Opponent("Captain America", "Captain America", "Vingadores: Guerra Infinita · Ultimato"),
                    new Opponent("Thor", "Thor", "Vingadores: Guerra Infinita · Ultimato"),
                    new Opponent("Hulk", "Hulk", "Vingadores: Guerra Infinita"),
                    new Opponent("Spider-Man", "Spider-Man", "Vingadores: Guerra Infinita · Ultimato"),
                    new Opponent("Black Panther", "Black Panther", "Vingadores: Guerra Infinita · Ultimato")));
        }
        // Reuse documented confrontations in both directions so the villain profile
        // stays in sync with the hero's enemy list, including display-name aliases.
        List<Opponent> heroes = new ArrayList<>();
        for (Opponent enemy : forName("Iron Man").opponents) {
            if (enemy.apiName.equalsIgnoreCase(name) || enemy.name.equalsIgnoreCase(name)) {
                heroes.add(new Opponent("Iron Man", "Iron Man", enemy.movies));
            }
        }
        return new McuProfile(Collections.emptyList(), heroes);
    }
}
