package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import com.google.gson.Gson;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class QuizGameTest {
    private List<Character> catalog() {
        List<Character> data = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            Character c = new Character(); c.id = i; c.name = "Character " + i;
            c.biography = new Character.Biography(); c.biography.publisher = "Marvel Comics";
            c.biography.alignment = i % 2 == 0 ? "good" : "bad";
            c.images = new Character.Images(); c.images.md = "https://example.com/" + i + ".jpg";
            c.powerstats = new Character.Powerstats(); c.powerstats.intelligence = 80; c.powerstats.strength = 50;
            data.add(c);
        }
        return data;
    }
    private QuizGame game() { return QuizGame.create(catalog(), "Misturado", new Random(4), null); }
    private void finish(QuizGame game) {
        for (int i = 0; i < 6; i++) { game.index = i; game.answer(game.current().target.id); }
        game.index = 6;
    }
    @Test public void blockHasSixDistinctTargetsAndFourUniqueChoices() {
        QuizGame game = game(); Set<Long> targets = new HashSet<>();
        assertEquals(6, game.questions.size());
        for (QuizGame.Question q : game.questions) {
            targets.add(q.target.id); Set<Long> options = new HashSet<>();
            q.choices.forEach(c -> options.add(c.id));
            assertEquals(4, options.size()); assertTrue(options.contains(q.target.id));
            assertFalse(q.clue().contains(q.target.name));
        }
        assertEquals(6, targets.size());
    }
    @Test public void categoriesAndIncompleteDataAreRespected() {
        for (Character c : QuizGame.eligible(catalog(), "Heróis")) assertFalse(c.isVillain());
        for (Character c : QuizGame.eligible(catalog(), "Vilões")) assertTrue(c.isVillain());
        List<Character> data = catalog(); data.get(0).images = null; data.get(1).biography.publisher = "DC";
        data.add(null); data.add(data.get(2));
        assertEquals(22, QuizGame.eligible(data, "Misturado").size());
    }
    @Test(expected = IllegalArgumentException.class) public void refusesAnIncompleteBlock() {
        QuizGame.create(catalog().subList(0, 5), "Misturado", new Random(), null);
    }
    @Test public void hintsReduceScoreAndAnswerCanOnlyBeRecordedOnce() {
        QuizGame game = game(); game.moreHint(); game.moreHint();
        game.answer(game.current().target.id); assertEquals(50, game.blockPoints());
        game.answer(-10); game.moreHint();
        assertEquals(50, game.blockPoints()); assertEquals(2, game.current().hint);
    }
    @Test public void wrongAndInvalidAnswersDoNotAwardPoints() {
        QuizGame game = game(); game.answer(-42); assertFalse(game.current().answered());
        long wrong = game.current().choices.stream().filter(c -> c.id != game.current().target.id).findFirst().get().id;
        game.answer(wrong); assertTrue(game.current().answered()); assertFalse(game.current().correct());
        assertEquals(0, game.blockPoints());
    }
    @Test public void hintCountIsCappedAndAllRaritiesAreReachable() {
        for (int count = 0; count < 4; count++) {
            QuizGame game = game(); for (int i = 0; i < count; i++) game.moreHint();
            game.answer(game.current().target.id); assertEquals(100 - count * 25, game.blockPoints());
        }
        QuizGame game = game(); for(int i=0;i<10;i++) game.moreHint(); assertEquals(25, game.current().value());
        assertEquals("Lendário", QuizGame.rarity(100)); assertEquals("Épico", QuizGame.rarity(75));
        assertEquals("Raro", QuizGame.rarity(50)); assertEquals("Comum", QuizGame.rarity(25));
    }
    @Test public void nextBlockKeepsTotalsWithoutRepeatingPreviousSix() {
        QuizGame first = game(); finish(first);
        QuizGame next = QuizGame.create(catalog(), "Misturado", new Random(2), first);
        assertEquals(600, next.totalPoints()); assertEquals(6, next.previousQuestions);
        Set<Long> old = new HashSet<>(); first.questions.forEach(q -> old.add(q.target.id));
        for (QuizGame.Question q : next.questions) assertFalse(old.contains(q.target.id));
        finish(next); assertEquals(1200, next.totalPoints()); assertEquals(12, next.totalCorrect());
    }
    @Test public void progressCreditsOnlyCompleteBlocksAndOnlyOnce() {
        PlayerProgress progress = new PlayerProgress(); QuizGame game = game();
        assertFalse(progress.credit(game, 1)); finish(game);
        assertTrue(progress.credit(game, 1)); assertFalse(progress.credit(game, 2));
        assertEquals(600, progress.points); assertEquals(1, progress.blocks);
        assertEquals(6, progress.collection.size()); assertEquals(1, progress.history.size());
    }
    @Test public void repeatedCharactersUpgradeButNeverDowngrade() {
        PlayerProgress progress = new PlayerProgress(); QuizGame weak = game();
        for (int i=0; i<6; i++) { weak.index=i; weak.moreHint(); weak.moreHint(); weak.moreHint(); weak.answer(weak.current().target.id); }
        progress.credit(weak, 1); QuizGame strong = game(); finish(strong); progress.credit(strong, 2);
        weak.id = "another-block"; progress.credit(weak, 3);
        assertEquals(6, progress.collection.size());
        for (PlayerProgress.Unlock u : progress.collection.values()) assertEquals(100, u.bestPoints);
    }
    @Test public void savedGameAndProfileRoundTripPreserveAnswerAndCreditedBlocks() {
        Gson gson = new Gson(); QuizGame game = game(); game.moreHint(); game.answer(game.current().target.id);
        QuizGame restored = gson.fromJson(gson.toJson(game), QuizGame.class);
        assertEquals(game.id, restored.id); assertEquals(75, restored.blockPoints()); assertTrue(restored.current().answered());
        finish(game); PlayerProgress progress = new PlayerProgress(); progress.credit(game, 123);
        progress.avatar = catalog().get(0);
        PlayerProgress read = gson.fromJson(gson.toJson(progress), PlayerProgress.class);
        assertEquals(1, read.avatar.id); assertFalse(read.credit(game, 456)); assertEquals(6, read.collection.size());
    }
}
