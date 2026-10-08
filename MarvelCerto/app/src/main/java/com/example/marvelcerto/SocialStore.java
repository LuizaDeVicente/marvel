package com.example.marvelcerto;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.marvelcerto.api.Character;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.*;

/** Local prototype store for social activity. A server can replace this boundary later. */
final class SocialStore {
    static final long MY_ID = 0L;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private Data data;

    static final class Data {
        List<Long> following = new ArrayList<>();
        List<Long> followers = new ArrayList<>();
        List<Post> posts = new ArrayList<>();
        List<Chat> chats = new ArrayList<>();
        List<Notice> notices = new ArrayList<>();
        List<Draft> drafts = new ArrayList<>();
        Map<Long, List<Long>> followsByAccount = new HashMap<>();
        int refreshCursor;
        boolean seeded;
    }
    static final class Post {
        String id;
        long authorId;
        String authorName;
        String caption;
        String photoPath;
        long date;
        int likes;
        boolean liked;
        boolean demoPost;
        boolean saved;
        List<String> likedBy = new ArrayList<>();
        List<Comment> comments = new ArrayList<>();
        String sharedPostId;
    }
    static final class Comment { String author; String text; long date; List<Comment> replies = new ArrayList<>(); boolean liked; int likes; }
    static final class Draft { String id; String caption; String photoPath; long updatedAt; }
    static final class Notice { String title; String detail; long date; boolean unread = true; String postId; }
    static final class Chat {
        long personId;
        String personName;
        List<Message> messages = new ArrayList<>();
    }
    static final class Message { String text; String postId; long date; boolean mine; }

    SocialStore(Context context) {
        prefs = context.getSharedPreferences("marvel_social", Context.MODE_PRIVATE);
        data = read();
    }
    private Data read() {
        try {
            Data parsed = gson.fromJson(prefs.getString("data", ""), Data.class);
            if (parsed == null) return new Data();
            boolean migrated = false;
            if (parsed.following == null) parsed.following = new ArrayList<>();
            if (parsed.followers == null) parsed.followers = new ArrayList<>();
            if (parsed.posts == null) parsed.posts = new ArrayList<>();
            if (parsed.chats == null) parsed.chats = new ArrayList<>();
            if (parsed.notices == null) parsed.notices = new ArrayList<>();
            if (parsed.drafts == null) { parsed.drafts = new ArrayList<>(); migrated = true; }
            if (parsed.followsByAccount == null) { parsed.followsByAccount = new HashMap<>(); migrated = true; }
            if (!parsed.followsByAccount.containsKey(MY_ID)) { parsed.followsByAccount.put(MY_ID, new ArrayList<>(parsed.following)); migrated = true; }
            for (Long follower : parsed.followers) {
                List<Long> relations = parsed.followsByAccount.get(follower);
                if (relations == null) { relations = new ArrayList<>(); parsed.followsByAccount.put(follower, relations); migrated = true; }
                if (!relations.contains(MY_ID)) { relations.add(MY_ID); migrated = true; }
            }
            for (Post post : parsed.posts) {
                if (post.likedBy == null) { post.likedBy = new ArrayList<>(); migrated = true; }
                if (post.authorId != MY_ID && !post.demoPost) { post.demoPost = true; migrated = true; }
                if (post.liked && !post.likedBy.contains("Capitão América")) { post.likedBy.add("Capitão América"); migrated = true; }
                if ("Luiza Vicente".equals(post.authorName)) { post.authorName = "Capitão América"; migrated = true; }
                if (post.comments != null) migrated |= migrateComments(post.comments);
            }
            for (Notice notice : parsed.notices) {
                if (notice.title != null && notice.title.contains("Luiza Vicente")) { notice.title = notice.title.replace("Luiza Vicente", "Capitão América"); migrated = true; }
                if (notice.detail != null && notice.detail.contains("Luiza Vicente")) { notice.detail = notice.detail.replace("Luiza Vicente", "Capitão América"); migrated = true; }
                if (notice.postId == null && !parsed.posts.isEmpty() && notice.title != null
                        && (notice.title.contains("curtiu") || notice.title.contains("coment"))) {
                    notice.postId = parsed.posts.get(0).id; migrated = true;
                }
            }
            if (migrated) prefs.edit().putString("data", gson.toJson(parsed)).apply();
            return parsed;
        } catch (RuntimeException e) { return new Data(); }
    }
    Data data() { return data; }
    void save() { prefs.edit().putString("data", gson.toJson(data)).apply(); }
    boolean follows(long id) { return data.following.contains(id); }
    List<Long> followingFor(long accountId) {
        List<Long> ids = data.followsByAccount.get(accountId);
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }
    List<Long> followersFor(long accountId) {
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> relation : data.followsByAccount.entrySet())
            if (relation.getValue() != null && relation.getValue().contains(accountId)) ids.add(relation.getKey());
        return ids;
    }
    private void setFollow(long follower, long followed, boolean active) {
        List<Long> ids = data.followsByAccount.get(follower);
        if (ids == null) { ids = new ArrayList<>(); data.followsByAccount.put(follower, ids); }
        if (active && !ids.contains(followed)) ids.add(followed);
        if (!active) ids.remove(followed);
    }
    void toggleFollow(Character person) {
        if (follows(person.id)) {
            data.following.remove(person.id);
            setFollow(MY_ID, person.id, false);
            data.notices.add(0, notice("Você deixou de seguir " + person.name, "A pessoa não aparecerá mais como seguida."));
        } else {
            data.following.add(person.id);
            setFollow(MY_ID, person.id, true);
            data.notices.add(0, notice("Agora você segue " + person.name, "Perfil adicionado à sua lista de seguindo."));
        }
        save();
    }
    void seed(List<Character> people) {
        if (people == null || people.isEmpty() || data.seeded) return;
        List<Character> marvel = new ArrayList<>();
        for (Character c : people) if (c.name != null && c.biography != null && c.biography.publisher != null
                && c.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) marvel.add(c);
        if (marvel.isEmpty()) return;
        List<Character> selected = new ArrayList<>();
        for (String needle : new String[]{"spider-man", "iron man", "captain america", "scarlet witch"}) {
            for (Character c : marvel) if (c.name.toLowerCase(Locale.ROOT).contains(needle)) { selected.add(c); break; }
        }
        for (int i = 0; i < marvel.size() && selected.size() < 4; i++) if (!selected.contains(marvel.get(i))) selected.add(marvel.get(i));
        for (int i = 0; i < Math.min(8, marvel.size()); i++) {
            Character c = marvel.get(i);
            if (i < 5 && !data.following.contains(c.id)) data.following.add(c.id);
            if (i > 1 && i < 6 && !data.followers.contains(c.id)) data.followers.add(c.id);
        }
        for (Character author : selected) if (!data.following.contains(author.id)) data.following.add(author.id);
        String[] captions = {
                "Um giro pelo bairro antes do fim do dia. Nova York sempre surpreende.",
                "No laboratório até tarde. A próxima versão vai ficar melhor.",
                "O trabalho em equipe continua sendo a melhor parte de tudo.",
                "Encontrando meu próprio caminho, um dia de cada vez."
        };
        for (int i = 0; i < Math.min(3, selected.size()); i++) {
            Character author = selected.get(i);
            Post post = new Post(); post.id = UUID.randomUUID().toString(); post.authorId = author.id;
            post.authorName = author.name; post.caption = captions[i]; post.date = System.currentTimeMillis() - (i + 1) * 3600000L; post.demoPost = true;
            post.likes = 23 + i * 41; post.comments = new ArrayList<>();
            post.likedBy = new ArrayList<>(Arrays.asList(i == 0 ? "Wanda Maximoff" : "Tony Stark", "Peter Parker"));
            Comment sample = new Comment(); sample.author = i == 0 ? "Tony Stark" : "Peter Parker";
            sample.text = i == 0 ? "A cidade está em boas mãos." : "Ficou incrível!"; sample.date = post.date + 90000;
            post.comments.add(sample); data.posts.add(post);
        }
        for (Long followed : data.following) setFollow(MY_ID, followed, true);
        for (Long follower : data.followers) setFollow(follower, MY_ID, true);
        for (int i = 0; i < selected.size(); i++) {
            Character from = selected.get(i), to = selected.get((i + 1) % selected.size());
            if (from.id != to.id) setFollow(from.id, to.id, true);
        }
        data.chats.clear();
        for (int i = 0; i < Math.min(3, selected.size()); i++) {
            Character person = selected.get(i); Chat chat = new Chat(); chat.personId = person.id; chat.personName = person.name;
            Message hello = new Message(); hello.text = new String[]{"Consegue falar um minuto?", "Te mando o relatório depois.", "Obrigada pela ajuda hoje."}[i];
            hello.date = System.currentTimeMillis() - (i + 1) * 1800000L; hello.mine = false; chat.messages.add(hello); data.chats.add(chat);
        }
        if (data.notices.isEmpty()) {
            Notice likedNotice = notice("Wanda curtiu sua publicação", "Toque para abrir a publicação · há 8 min");
            if (!data.posts.isEmpty()) likedNotice.postId = data.posts.get(0).id;
            data.notices.add(likedNotice);
            data.notices.add(notice("Sam Wilson começou a seguir você", "há 42 min"));
            Notice commentNotice = notice("Peter comentou na sua publicação", "Toque para abrir a publicação · há 2 h");
            if (!data.posts.isEmpty()) commentNotice.postId = data.posts.get(0).id;
            data.notices.add(commentNotice);
        }
        data.seeded = true; save();
    }
    Post createPost(String caption, String photoPath, Character avatar) {
        Post post = new Post(); post.id = UUID.randomUUID().toString(); post.authorId = MY_ID;
        post.authorName = "Capitão América"; post.caption = caption; post.photoPath = photoPath;
        post.date = System.currentTimeMillis(); post.likes = 0; post.comments = new ArrayList<>();
        data.posts.add(0, post); save(); return post;
    }
    void editPost(Post post, String caption) {
        if (post == null || post.authorId != MY_ID) return;
        post.caption = caption; save();
    }
    void toggleSaved(Post post) { if (post != null) { post.saved = !post.saved; save(); } }
    Draft saveDraft(String id, String caption, String photoPath) {
        Draft draft = null;
        if (id != null) for (Draft existing : data.drafts) if (Objects.equals(existing.id, id)) { draft = existing; break; }
        if (draft == null) { draft = new Draft(); draft.id = UUID.randomUUID().toString(); data.drafts.add(0, draft); }
        draft.caption = caption; draft.photoPath = photoPath; draft.updatedAt = System.currentTimeMillis();
        save(); return draft;
    }
    void deleteDraft(String id) { data.drafts.removeIf(draft -> Objects.equals(draft.id, id)); save(); }
    void deletePost(Post post) {
        if (post == null || post.authorId != MY_ID) return;
        data.posts.removeIf(existing -> Objects.equals(existing.id, post.id));
        data.notices.removeIf(notice -> Objects.equals(notice.postId, post.id));
        for (Chat chat : data.chats) if (chat.messages != null)
            chat.messages.removeIf(message -> Objects.equals(message.postId, post.id));
        save();
    }
    void toggleLike(Post post) {
        if (post.likedBy == null) post.likedBy = new ArrayList<>();
        post.liked = !post.liked; post.likes = Math.max(0, post.likes + (post.liked ? 1 : -1));
        if (post.liked && !post.likedBy.contains("Capitão América")) post.likedBy.add("Capitão América");
        if (!post.liked) post.likedBy.remove("Capitão América");
        save();
    }
    void toggleLike(Comment comment) { comment.liked = !comment.liked; comment.likes = Math.max(0, comment.likes + (comment.liked ? 1 : -1)); save(); }
    void comment(Post post, String text) {
        Comment comment = new Comment(); comment.author = "Capitão América"; comment.text = text; comment.date = System.currentTimeMillis();
        if (post.comments == null) post.comments = new ArrayList<>(); post.comments.add(comment); save();
        if (post.authorId == MY_ID) { data.notices.add(0, notice("Novo comentário na sua publicação", "Capitão América: " + text, post.id)); save(); }
    }
    int addCommunityPosts(List<Character> people) {
        if (people == null || people.isEmpty()) return 0;
        int added = 0;
        int cursor = Math.floorMod(data.refreshCursor, people.size());
        String[] captions = {"Um novo dia, uma nova missão.", "A equipe segue protegendo o universo.", "Bastidores de mais uma aventura Marvel.", "Sempre pronto para o próximo desafio."};
        int attempts = 0;
        Set<Long> recentAuthors = new HashSet<>(); int recentCount = 0;
        for (Post existing : data.posts) {
            if (!existing.demoPost) continue;
            recentAuthors.add(existing.authorId);
            if (++recentCount >= 12) break;
        }
        while (added < 4 && attempts < people.size()) {
            Character person = people.get((cursor + attempts) % people.size()); attempts++;
            if (person == null || person.id == MY_ID || person.name == null || recentAuthors.contains(person.id)) continue;
            Post post = new Post(); post.id = UUID.randomUUID().toString(); post.authorId = person.id;
            post.authorName = person.name; post.caption = captions[(data.refreshCursor + added) % captions.length];
            post.date = System.currentTimeMillis() - (added * 45000L); post.likes = 4 + Math.floorMod(person.name.hashCode(), 92); post.demoPost = true;
            post.likedBy = new ArrayList<>(Arrays.asList("Peter Parker", "Wanda Maximoff"));
            post.comments = new ArrayList<>(); data.posts.add(0, post); added++;
            recentAuthors.add(person.id);
        }
        data.refreshCursor = (cursor + attempts) % people.size();
        save(); return added;
    }
    void reply(Post post, Comment parent, String text) {
        if (parent == null) { comment(post, text); return; }
        Comment reply = new Comment(); reply.author = "Capitão América"; reply.text = text; reply.date = System.currentTimeMillis();
        if (parent.replies == null) parent.replies = new ArrayList<>(); parent.replies.add(reply); save();
        if (post.authorId == MY_ID) { data.notices.add(0, notice("Nova resposta na sua publicação", "Capitão América: " + text, post.id)); save(); }
    }
    private boolean migrateComments(List<Comment> comments) {
        boolean changed = false;
        for (Comment comment : comments) {
            if (comment.replies == null) comment.replies = new ArrayList<>();
            if ("Luiza Vicente".equals(comment.author)) { comment.author = "Capitão América"; changed = true; }
            changed |= migrateComments(comment.replies);
        }
        return changed;
    }
    Chat chat(Character person) {
        for (Chat c : data.chats) if (c.personId == person.id) return c;
        Chat c = new Chat(); c.personId = person.id; c.personName = person.name; c.messages = new ArrayList<>(); data.chats.add(0, c); save(); return c;
    }
    void send(Chat chat, String text, String postId) {
        Message message = new Message(); message.text = text; message.postId = postId; message.date = System.currentTimeMillis(); message.mine = true;
        chat.messages.add(message); save();
    }
    void markRead() { for (Notice n : data.notices) n.unread = false; save(); }
    int unreadCount() { int n = 0; for (Notice notice : data.notices) if (notice.unread) n++; return n; }
    Post findPost(String id) { for (Post p : data.posts) if (Objects.equals(p.id, id)) return p; return null; }
    private static Notice notice(String title, String detail) { return notice(title, detail, null); }
    private static Notice notice(String title, String detail, String postId) { Notice n = new Notice(); n.title = title; n.detail = detail; n.postId = postId; n.date = System.currentTimeMillis(); return n; }
}
