package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import java.util.*;

/** One transaction per completed block; duplicate submission never awards points twice. */
public final class PlayerProgress {
    public long points;
    public int blocks;
    public int correct;
    public Character avatar;
    public Map<Long, Unlock> collection = new LinkedHashMap<>();
    /** Number of distinct question variants already shown for each character. */
    public Map<Long, Integer> questionCounts = new HashMap<>();
    public List<Match> history = new ArrayList<>();
    public Set<String> credited = new HashSet<>();
    public static class Unlock {
        public Character character;
        public int bestPoints;
    }
    public static class Match {
        public long date;
        public int points;
        public int correct;
        public String category;
    }
    public boolean credit(QuizGame game, long now) {
        if (!game.complete() || !credited.add(game.id)) return false;
        points += game.blockPoints(); blocks++; correct += game.blockCorrect();
        Match match = new Match();
        match.date = now; match.points = game.blockPoints(); match.correct = game.blockCorrect(); match.category = game.category;
        history.add(0, match);
        if (history.size() > 30) history.remove(history.size() - 1);
        for (QuizGame.Question q : game.questions) {
            if (!q.correct()) continue;
            Unlock unlock = collection.get(q.target.id);
            if (unlock == null) { unlock = new Unlock(); unlock.character = q.target; collection.put(q.target.id, unlock); }
            unlock.bestPoints = Math.max(unlock.bestPoints, q.points);
        }
        return true;
    }
}
