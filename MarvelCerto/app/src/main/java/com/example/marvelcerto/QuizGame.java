package com.example.marvelcerto;

import com.example.marvelcerto.api.Character;
import java.util.*;

/** Android-independent quiz rules; a block always contains six distinct answers. */
public final class QuizGame {
    public static final int BLOCK_SIZE = 6;
    public String id = UUID.randomUUID().toString();
    public String category;
    public List<Question> questions = new ArrayList<>();
    public int index;
    public boolean result;
    public int previousPoints;
    public int previousCorrect;
    public int previousQuestions;
    public static final class Question {
        public Character target;
        public List<Character> choices;
        public int variant;
        public String firstClue;
        public int hint;
        public long selectedId = -1;
        public int points;
        public boolean answered() { return selectedId != -1; }
        public boolean correct() { return answered() && selectedId == target.id; }
        public int value() { return 100 - hint * 25; }
        public String clue() {
            if (hint == 0) return firstClue == null ? initialClue(target, variant) : firstClue;
            if (hint == 1) return secondaryClue(target, variant);
            return hint == 2 ? "Reconhece este detalhe do retrato?" : "Última pista: veja o retrato completo.";
        }
        public String explanation() {
            if (variant == 0) {
                String group = affiliation(target);
                if (!group.isEmpty()) return "A pista citava " + group + ". A ficha de " + target.name + " confirma essa afiliação.";
                return "A pista citava a primeira aparição. A ficha de " + target.name + " registra "
                        + textOr(target.biography.firstAppearance, "uma história da Marvel") + ".";
            }
            if (variant == 1) {
                String alias = identity(target);
                if (!alias.isEmpty()) return "A pista usava o nome " + alias + ". A ficha registra esse nome como identidade ou apelido de " + target.name + ".";
                return "A pista era a primeira aparição. A ficha de " + target.name + " registra "
                        + textOr(target.biography.firstAppearance, "uma história da Marvel") + ".";
            }
            int index = statIndex(target);
            return "A pista perguntava por " + statName(index) + " " + statValue(target, index)
                    + ". Esse é o valor registrado para " + target.name + " na ficha.";
        }
        public String curiosity() {
            if (variant == 0) {
                String name = identity(target);
                return !name.isEmpty() ? "Curiosidade: a ficha também registra " + name + " como identidade ou apelido."
                        : "Curiosidade: sua primeira aparição foi " + textOr(target.biography.firstAppearance, "não informada") + ".";
            }
            if (variant == 1) {
                String group = affiliation(target);
                return !group.isEmpty() ? "Curiosidade: a ficha também associa " + target.name + " a " + group + "."
                        : "Curiosidade: sua primeira aparição foi " + textOr(target.biography.firstAppearance, "não informada") + ".";
            }
            String group = affiliation(target);
            if (!group.isEmpty()) return "Curiosidade: além desse atributo, " + target.name + " tem ligação com " + group + ".";
            String name = identity(target);
            return !name.isEmpty() ? "Curiosidade: a ficha também registra " + name + " como identidade ou apelido."
                    : "Curiosidade: sua primeira aparição foi " + textOr(target.biography.firstAppearance, "não informada") + ".";
        }
    }
    public static List<Character> eligible(List<Character> source, String category) {
        return eligible(source, category, Collections.emptyMap());
    }
    public static List<Character> eligible(List<Character> source, String category, Map<Long, Integer> asked) {
        Map<String, Character> unique = new LinkedHashMap<>();
        for (Character c : source) {
            if (c == null || c.id <= 0 || c.name == null || c.name.trim().isEmpty() || c.getImageUrl() == null
                    || c.powerstats == null || c.biography == null || c.biography.publisher == null
                    || !c.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) continue;
            if (asked != null && asked.getOrDefault(c.id, 0) >= 3) continue;
            boolean hero = "good".equalsIgnoreCase(c.biography.alignment);
            if (!hero && !c.isVillain()) continue;
            if ("Heróis".equals(category) && !hero || "Vilões".equals(category) && !c.isVillain()) continue;
            unique.putIfAbsent(c.name.toLowerCase(Locale.ROOT).trim(), c);
        }
        return new ArrayList<>(unique.values());
    }
    public static QuizGame create(List<Character> source, String category, Random random, QuizGame previous) {
        return create(source, category, random, previous, Collections.emptyMap());
    }
    public static QuizGame create(List<Character> source, String category, Random random, QuizGame previous, Map<Long, Integer> asked) {
        List<Character> available = eligible(source, category, Collections.emptyMap());
        List<Character> pool = eligible(source, category, asked);
        if (pool.size() < BLOCK_SIZE) {
            String message = available.size() < BLOCK_SIZE
                    ? "São necessários pelo menos 6 personagens com dados suficientes nesta categoria."
                    : "Esta categoria não tem mais 6 personagens com perguntas inéditas. Troque a categoria para continuar.";
            throw new IllegalArgumentException(message);
        }
        QuizGame game = new QuizGame();
        game.category = category;
        if (previous != null) {
            game.previousPoints = previous.totalPoints();
            game.previousCorrect = previous.totalCorrect();
            game.previousQuestions = previous.previousQuestions + BLOCK_SIZE;
        }
        List<Character> targets = new ArrayList<>(pool);
        if (previous != null && pool.size() >= 12) {
            Set<Long> last = new HashSet<>();
            for (Question q : previous.questions) last.add(q.target.id);
            targets.removeIf(c -> last.contains(c.id));
        }
        Collections.shuffle(targets, random);
        for (int i = 0; i < BLOCK_SIZE; i++) {
            Question q = new Question();
            q.target = targets.get(i);
            q.variant = asked == null ? 0 : Math.min(2, Math.max(0, asked.getOrDefault(q.target.id, 0)));
            q.firstClue = initialClue(q.target, q.variant);
            List<Character> alternatives = new ArrayList<>(pool);
            alternatives.removeIf(c -> c.id == q.target.id);
            Collections.shuffle(alternatives, random);
            q.choices = new ArrayList<>(alternatives.subList(0, 3));
            q.choices.add(q.target);
            Collections.shuffle(q.choices, random);
            game.questions.add(q);
        }
        return game;
    }
    private static String initialClue(Character c, int variant) {
        if (variant == 0) {
            String group = affiliation(c);
            if (!group.isEmpty()) return "Pista de equipe: quem tem ligação com " + group + "?";
            String debut = textOr(c.biography.firstAppearance, "histórias da Marvel");
            if (c.name != null && debut.toLowerCase(Locale.ROOT).contains(c.name.toLowerCase(Locale.ROOT))) debut = "histórias da Marvel";
            return "Pista de trajetória: quem apareceu pela primeira vez em " + debut + "?";
        }
        if (variant == 1) {
            String identity = identity(c);
            if (!identity.isEmpty()) return "Pista de identidade: " + identity + " é um nome ligado a qual personagem?";
            String debut = textOr(c.biography.firstAppearance, "histórias da Marvel");
            if (c.name != null && debut.toLowerCase(Locale.ROOT).contains(c.name.toLowerCase(Locale.ROOT))) debut = "histórias da Marvel";
            return "Pista de primeira aparição: quem estreou em " + debut + "?";
        }
        int stat = statIndex(c);
        return "Pista de atributo: quem tem " + statName(stat) + " " + statValue(c, stat) + " na ficha?";
    }
    private static String secondaryClue(Character c, int variant) {
        if (variant == 0) return "Inteligência: " + statValue(c, 0) + "/100 · Força: " + statValue(c, 1) + "/100";
        if (variant == 1) {
            String group = affiliation(c);
            return group.isEmpty() ? "É " + (c.isVillain() ? "um vilão" : "um herói") + " do universo Marvel." : "Também tem ligação com " + group + ".";
        }
        String identity = identity(c);
        return identity.isEmpty() ? "É " + (c.isVillain() ? "um vilão" : "um herói") + " do universo Marvel." : "Identidade ou nome alternativo: " + identity + ".";
    }
    private static String affiliation(Character c) {
        String group = c.connections == null ? "" : textOr(c.connections.groupAffiliation, "");
        if (group.isEmpty() || group.equals("—")) return "";
        String first = group.split("[,;\\n]", 2)[0].trim();
        return first.isEmpty() || c.name != null && first.equalsIgnoreCase(c.name) ? "" : "\"" + first + "\"";
    }
    private static String identity(Character c) {
        if (c.biography == null || c.name == null) return "";
        String full = textOr(c.biography.fullName, "");
        if (!full.isEmpty() && !full.equalsIgnoreCase(c.name) && !full.equals("—")) return full;
        if (c.biography.aliases != null) for (String alias : c.biography.aliases) {
            if (alias != null && !alias.trim().isEmpty() && !alias.equalsIgnoreCase(c.name) && !alias.equals("-")) return alias.trim();
        }
        return "";
    }
    private static String textOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() || value.trim().equals("-") || value.trim().equals("—") ? fallback : value.trim();
    }
    private static int statIndex(Character c) {
        return (int)(Math.abs(c.id) % 6);
    }
    private static String statName(int i) {
        return new String[]{"Inteligência", "Força", "Velocidade", "Durabilidade", "Poder", "Combate"}[i];
    }
    private static int statValue(Character c, int i) {
        int[] values = {c.powerstats.intelligence, c.powerstats.strength, c.powerstats.speed,
                c.powerstats.durability, c.powerstats.power, c.powerstats.combat};
        return values[i];
    }
    public Question current() { return questions.get(index); }
    public void moreHint() { if (!current().answered() && current().hint < 3) current().hint++; }
    public void answer(long id) {
        Question q = current();
        if (q.answered() || q.choices.stream().noneMatch(c -> c.id == id)) return;
        q.selectedId = id;
        q.points = q.correct() ? q.value() : 0;
    }
    public boolean complete() { return questions.size() == BLOCK_SIZE && questions.stream().allMatch(Question::answered); }
    public int blockPoints() { return questions.stream().mapToInt(q -> q.points).sum(); }
    public int blockCorrect() { return (int)questions.stream().filter(Question::correct).count(); }
    public int totalPoints() { return previousPoints + blockPoints(); }
    public int totalCorrect() { return previousCorrect + blockCorrect(); }
    public static String rarity(int points) {
        if (points >= 100) return "Lendário";
        if (points >= 75) return "Épico";
        if (points >= 50) return "Raro";
        return "Comum";
    }
}
