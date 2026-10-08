package com.example.marvelcerto;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.example.marvelcerto.api.Character;
import com.example.marvelcerto.api.MarvelApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** MarvelVerse social experience. Social interactions are persisted locally for this prototype. */
public class MainActivity extends AppCompatActivity {
    private FrameLayout root;
    private LinearLayout shell, pageBody, bottomBar;
    private ScrollView scroll;
    private SocialStore social;
    private GameStore gameStore;
    private final List<Character> characters = new ArrayList<>();
    private Call<List<Character>> request;
    private String page = "feed";
    private Character selectedPerson, activeChat;
    private SocialStore.Post sharingPost;
    private Uri pendingPhoto;
    private Bitmap pendingBitmap;
    private ImageView photoPreview;
    private EditText postText;
    private boolean fetching;
    private boolean refreshingFeed;
    private boolean refreshOnResume;
    private View sidebarOverlay;
    private String peopleQuery = "";
    private String feedMode = "Para você";
    private String activeDraftId;
    private String draftPhotoPath;
    private long relationOwnerId = SocialStore.MY_ID;
    private String focusedPostId;
    private ActivityResultLauncher<String> galleryPicker;
    private ActivityResultLauncher<Void> cameraPicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        social = new SocialStore(this); gameStore = new GameStore(this);
        if (state != null) { page = state.getString("social_page", "feed"); peopleQuery = state.getString("people_query", ""); }
        else page = getIntent().getStringExtra("open_page") == null ? "feed" : getIntent().getStringExtra("open_page");
        galleryPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return; pendingPhoto = uri; pendingBitmap = null; updatePhotoPreview();
        });
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap == null) return; pendingBitmap = bitmap; pendingPhoto = null; updatePhotoPreview();
        });
        buildShell();
        for (Character c : gameStore.catalog()) if (isMarvel(c)) characters.add(c);
        social.seed(characters);
        render();
        loadCharacters();
    }

    private void buildShell() {
        root = new FrameLayout(this); root.setBackgroundColor(color(R.color.ink));
        shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        shell.addView(toolbar(), new LinearLayout.LayoutParams(-1, dp(58)));
        scroll = new ScrollView(this) {
            private float pullStartY;
            private boolean refreshGesture;
            @Override public boolean onInterceptTouchEvent(android.view.MotionEvent event) {
                if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) { pullStartY = event.getY(); refreshGesture = false; }
                if (page.equals("feed") && !fetching && getScrollY() == 0 && event.getActionMasked() == android.view.MotionEvent.ACTION_MOVE
                        && event.getY() - pullStartY > dp(88)) { refreshGesture = true; return true; }
                return super.onInterceptTouchEvent(event);
            }
            @Override public boolean onTouchEvent(android.view.MotionEvent event) {
                if (refreshGesture) {
                    if (event.getActionMasked() == android.view.MotionEvent.ACTION_UP) { refreshGesture = false; refreshFeed(); }
                    return true;
                }
                return super.onTouchEvent(event);
            }
        };
        scroll.setFillViewport(true); scroll.setClipToPadding(false);
        pageBody = new LinearLayout(this); pageBody.setOrientation(LinearLayout.VERTICAL);
        pageBody.setPadding(dp(16), dp(10), dp(16), dp(22));
        scroll.addView(pageBody, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        bottomBar = new LinearLayout(this); bottomBar.setGravity(Gravity.CENTER); bottomBar.setBackgroundColor(color(R.color.ink_2));
        shell.addView(bottomBar, new LinearLayout.LayoutParams(-1, dp(64)));
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        setContentView(root);
    }

    private View toolbar() {
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(12), 0, dp(10), 0);
        bar.setBackgroundColor(color(R.color.ink_2));
        ImageButton menu = iconButton(R.drawable.ic_social_menu, "Abrir menu"); bar.addView(menu, new LinearLayout.LayoutParams(dp(42), dp(48)));
        menu.setOnClickListener(v -> openSidebar());
        TextView brand = new TextView(this); brand.setText("MARVELVERSE"); brand.setTextColor(Color.WHITE); brand.setTextSize(19); brand.setTypeface(null, Typeface.BOLD);
        brand.setLetterSpacing(.04f); LinearLayout.LayoutParams weight = new LinearLayout.LayoutParams(0, -2, 1); weight.leftMargin = dp(7); bar.addView(brand, weight);
        ImageButton notifications = iconButton(R.drawable.ic_social_bell, "Notificações, " + social.unreadCount() + " não lidas");
        bar.addView(notifications, new LinearLayout.LayoutParams(dp(42), dp(48))); notifications.setOnClickListener(v -> { page = "notifications"; social.markRead(); render(); });
        ImageButton chats = iconButton(R.drawable.ic_social_chat, "Conversas"); bar.addView(chats, new LinearLayout.LayoutParams(dp(42), dp(48)));
        chats.setOnClickListener(v -> { page = "chats"; render(); });
        return bar;
    }

    private ImageButton iconButton(int icon, String description) {
        ImageButton button = new ImageButton(this); button.setImageResource(icon); button.setPadding(dp(11), dp(11), dp(11), dp(11));
        button.setScaleType(ImageView.ScaleType.FIT_CENTER); button.setBackgroundResource(R.drawable.bg_social_icon);
        button.setContentDescription(description); button.setClickable(true); button.setFocusable(true);
        return button;
    }

    private void render() {
        pageBody.removeAllViews(); bottomBar.removeAllViews();
        if (page.equals("feed")) renderFeed();
        else if (page.equals("people")) renderPeople();
        else if (page.equals("profile")) renderProfile();
        else if (page.equals("games")) renderGames();
        else if (page.equals("saved")) renderSavedPosts();
        else if (page.equals("drafts")) renderDrafts();
        else if (page.equals("search")) renderSearch();
        else if (page.equals("followers") || page.equals("following")) renderRelations(page.equals("followers"));
        else if (page.equals("person")) renderPerson();
        else if (page.equals("chats")) renderChats();
        else if (page.equals("chat")) renderChat();
        else if (page.equals("notifications")) renderNotifications();
        else if (page.equals("compose")) renderComposer();
        buildBottomBar();
    }

    private void buildBottomBar() {
        String[] ids = {"feed", "people", "compose", "chats", "profile"};
        String[] glyphs = {"⌂", "♧", "+", "▤", ""};
        String[] titles = {"Início", "Amigos", "Criar", "Chats", "Perfil"};
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
            item.setClickable(true); item.setFocusable(true); item.setContentDescription(titles[i]);
            View icon;
            if (id.equals("profile")) {
                ImageView personIcon = new ImageView(this); personIcon.setImageResource(R.drawable.ic_profile);
                personIcon.setColorFilter(color(id.equals(page) ? R.color.marvel_red : R.color.text_secondary));
                personIcon.setContentDescription("Perfil");
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(23), dp(23));
                iconParams.topMargin = dp(2); iconParams.bottomMargin = dp(2); icon = personIcon;
                item.addView(icon, iconParams);
            } else {
                TextView textIcon = new TextView(this); textIcon.setText(glyphs[i]); textIcon.setGravity(Gravity.CENTER); textIcon.setTextSize(i == 2 ? 27 : 21);
                textIcon.setTextColor(color(id.equals(page) ? R.color.marvel_red : R.color.text_secondary));
                icon = textIcon;
            }
            TextView label = new TextView(this); label.setText(titles[i]); label.setTextSize(10); label.setGravity(Gravity.CENTER);
            label.setTextColor(color(id.equals(page) ? R.color.marvel_red : R.color.text_secondary));
            if (id.equals("compose")) {
                TextView plus = (TextView) icon;
                plus.setTextColor(Color.WHITE); android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL); circle.setColor(color(R.color.marvel_red));
                plus.setBackground(circle); plus.setIncludeFontPadding(false);
                item.addView(plus, new LinearLayout.LayoutParams(dp(38), dp(38)));
            } else if (!id.equals("profile")) item.addView(icon);
            item.addView(label);
            item.setOnClickListener(v -> { if (id.equals("compose")) startNewPost(); else page = id; render(); });
            bottomBar.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private void renderFeed() {
        LinearLayout feedHeading = new LinearLayout(this); feedHeading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout feedTitles = new LinearLayout(this); feedTitles.setOrientation(LinearLayout.VERTICAL);
        feedTitles.addView(addText("Feed", 23, true)); feedTitles.addView(addText("Publicações da comunidade Marvel", 13, false));
        feedHeading.addView(feedTitles, new LinearLayout.LayoutParams(0, -2, 1));
        TextView refresh = addText("↻", 25, true); refresh.setTextColor(color(R.color.marvel_red)); refresh.setGravity(Gravity.CENTER);
        refresh.setMinimumWidth(dp(48)); refresh.setMinimumHeight(dp(48)); refresh.setContentDescription("Atualizar feed");
        refresh.setClickable(true); refresh.setFocusable(true); refresh.setOnClickListener(v -> refreshFeed()); feedHeading.addView(refresh);
        pageBody.addView(feedHeading);
        LinearLayout modes = new LinearLayout(this); modes.setGravity(Gravity.CENTER); modes.setPadding(0, dp(8), 0, dp(6));
        addFeedMode(modes, "Para você"); addFeedMode(modes, "Seguindo"); pageBody.addView(modes);
        if (refreshingFeed) {
            LinearLayout loading = new LinearLayout(this); loading.setGravity(Gravity.CENTER); loading.setPadding(0, dp(8), 0, dp(8));
            ProgressBar spinner = new ProgressBar(this); spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(color(R.color.marvel_red)));
            loading.addView(spinner, new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView label = addText("  Buscando novas publicações…", 12, false); loading.addView(label); pageBody.addView(loading);
        }
        renderStories();
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Buscar posts ou personagens"); search.setTextColor(Color.WHITE);
        search.setHintTextColor(color(R.color.text_secondary)); search.setPadding(dp(14), 0, dp(14), 0); search.setBackgroundResource(R.drawable.bg_social_input);
        search.setContentDescription("Buscar publicações e personagens"); search.setOnEditorActionListener((view, actionId, event) -> {
            String query = search.getText().toString().trim(); if (!query.isEmpty()) { peopleQuery = query; page = "search"; render(); } return true;
        });
        search.setOnClickListener(v -> { if (search.getText().length() == 0) { page = "search"; render(); } });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(48)); searchParams.topMargin = dp(8); pageBody.addView(search, searchParams);
        if (fetching && characters.isEmpty()) addInfo("Carregando personagens…", false);
        else if (characters.isEmpty()) { addInfo("Não foi possível carregar os personagens. Confira sua conexão e tente novamente.", true); addButton(pageBody, "Tentar novamente", false, this::loadCharacters); }
        if (!fetching && characters.isEmpty()) return;
        int shownPosts = 0;
        for (SocialStore.Post post : social.data().posts) {
            if (feedMode.equals("Seguindo") && post.authorId != SocialStore.MY_ID && !social.follows(post.authorId)) continue;
            addPost(post); shownPosts++;
        }
        if (!fetching && shownPosts == 0) empty(feedMode.equals("Seguindo") ? "Seu feed de seguindo está vazio" : "Seu feed está começando",
                feedMode.equals("Seguindo") ? "Siga personagens para ver as publicações deles aqui." : "Siga personagens ou publique uma novidade para preencher o feed.");
        if (!fetching && !characters.isEmpty()) addText("Atualizado com personagens da API Marvel", 11, false).setGravity(Gravity.CENTER);
    }

    private void addFeedMode(LinearLayout parent, String label) {
        TextView chip = addText(label, 13, true); chip.setGravity(Gravity.CENTER); chip.setMinimumHeight(dp(42)); chip.setContentDescription("Mostrar feed: " + label);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(); bg.setCornerRadius(dp(14));
        boolean selected = feedMode.equals(label); bg.setColor(color(selected ? R.color.marvel_red : R.color.ink_3)); chip.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1); params.setMargins(dp(4), 0, dp(4), 0); parent.addView(chip, params);
        chip.setClickable(true); chip.setFocusable(true); chip.setOnClickListener(v -> { feedMode = label; render(); });
    }

    private void renderStories() {
        LinearLayout strip = new LinearLayout(this); strip.setGravity(Gravity.CENTER_VERTICAL);
        HorizontalScrollView horizontal = new HorizontalScrollView(this); horizontal.setHorizontalScrollBarEnabled(false);
        List<Character> shown = new ArrayList<>();
        for (Long id : social.data().following) { Character c = person(id); if (c != null) shown.add(c); if (shown.size() == 7) break; }
        if (shown.isEmpty()) for (int i = 0; i < Math.min(7, characters.size()); i++) shown.add(characters.get(i));
        for (Character c : shown) {
            LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(dp(6), dp(4), dp(6), dp(6));
            item.addView(avatarView(c, 56));
            TextView label = addText(c.name, 10, false); label.setGravity(Gravity.CENTER); label.setMaxWidth(dp(64)); item.addView(label);
            item.setContentDescription("Abrir perfil de " + c.name); item.setClickable(true);
            item.setOnClickListener(v -> { selectedPerson = c; page = "person"; render(); }); strip.addView(item);
        }
        horizontal.addView(strip); pageBody.addView(horizontal);
    }

    private void addPost(SocialStore.Post post) {
        LinearLayout box = card(); box.setPadding(dp(12), dp(11), dp(12), dp(8));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        Character author = person(post.authorId);
        ImageView portrait = avatarView(post.authorId == SocialStore.MY_ID ? gameStore.progress().avatar : author, 42); header.addView(portrait);
        LinearLayout details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(10), 0, 0, 0);
        TextView name = addText(post.authorName == null ? "Capitão América" : post.authorName, 15, true); details.addView(name);
        details.addView(addText(relative(post.date), 11, false));
        header.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        TextView more = addText("···", 20, true); more.setGravity(Gravity.CENTER); more.setMinimumWidth(dp(48)); more.setMinimumHeight(dp(48));
        if (post.authorId == SocialStore.MY_ID) {
            more.setContentDescription("Opções da sua publicação"); more.setClickable(true); more.setFocusable(true);
            more.setOnClickListener(v -> showPostOptions(post)); header.addView(more);
        }
        String authorDescription = post.authorId == SocialStore.MY_ID ? "Abrir perfil de Capitão América" : "Abrir perfil de " + (author == null ? post.authorName : author.name);
        header.setContentDescription(authorDescription); header.setClickable(true); header.setFocusable(true);
        header.setMinimumHeight(dp(48));
        header.setOnClickListener(v -> openPostAuthor(post, author));
        portrait.setOnClickListener(v -> openPostAuthor(post, author)); portrait.setClickable(true); portrait.setFocusable(true);
        portrait.setContentDescription(authorDescription);
        box.addView(header); box.addView(addText(post.caption == null ? "" : post.caption, 14, false));
        if (post.demoPost) {
            TextView demo = addText("Publicação de exemplo · personagem da API Marvel", 10, false);
            demo.setTextColor(color(R.color.text_muted)); box.addView(demo);
        }
        if (post.photoPath != null && !post.photoPath.isEmpty()) {
            ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP); photo.setContentDescription("Foto da publicação de " + post.authorName);
            box.addView(photo, new LinearLayout.LayoutParams(-1, dp(230)));
            Glide.with(this).load(new File(post.photoPath)).centerCrop().placeholder(R.drawable.bg_camera_image).error(R.drawable.bg_camera_image).into(photo);
        } else if (author != null && author.getImageUrl() != null) {
            ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP); photo.setContentDescription("Personagem da publicação: " + post.authorName);
            box.addView(photo, new LinearLayout.LayoutParams(-1, dp(230)));
            Glide.with(this).load(author.getImageUrl()).centerCrop().placeholder(R.drawable.bg_camera_image).error(R.drawable.bg_camera_image).into(photo);
        }
        LinearLayout stats = new LinearLayout(this); stats.setGravity(Gravity.CENTER_VERTICAL); stats.setPadding(0, dp(7), 0, dp(3));
        TextView likes = addText("★ " + post.likes + " curtidas · ver quem curtiu", 12, false); likes.setTextColor(color(R.color.marvel_gold));
        likes.setContentDescription(post.likes + " curtidas. Toque para ver quem curtiu."); likes.setClickable(true); likes.setFocusable(true); likes.setMinHeight(dp(44));
        likes.setOnClickListener(v -> showLikers(post));
        stats.addView(likes, new LinearLayout.LayoutParams(0, -2, 1));
        TextView commentsLabel = addText(commentCount(post.comments) + " comentários", 12, false);
        commentsLabel.setContentDescription(commentCount(post.comments) + " comentários, incluindo respostas");
        stats.addView(commentsLabel); box.addView(stats);
        View divider = new View(this); divider.setBackgroundColor(color(R.color.divider)); box.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        action(actions, post.liked ? "♥  Curtido" : "♡  Curtir", () -> { social.toggleLike(post); render(); }, post.liked);
        action(actions, "▢  Comentar", () -> showComments(post), false);
        saveAction(actions, post.saved, () -> { social.toggleSaved(post); render(); });
        action(actions, "↗  Enviar", () -> sharePost(post), false); box.addView(actions);
        int start = Math.max(0, (post.comments == null ? 0 : post.comments.size()) - 2);
        for (int i = start; post.comments != null && i < post.comments.size(); i++) {
            SocialStore.Comment comment = post.comments.get(i); TextView line = addText(comment.author + ": " + comment.text, 12, false);
            line.setPadding(dp(6), dp(4), dp(6), dp(4)); box.addView(line);
            if (comment.replies != null) for (SocialStore.Comment reply : comment.replies) {
                TextView replyLine = addText("↳  " + reply.author + ": " + reply.text, 11, false);
                replyLine.setPadding(dp(22), dp(3), dp(6), dp(3)); box.addView(replyLine);
            }
        }
        pageBody.addView(box, verticalMargin(12));
        box.setTag(post.id);
        if (page.equals("feed") && Objects.equals(focusedPostId, post.id)) {
            focusedPostId = null;
            scroll.post(() -> scroll.smoothScrollTo(0, box.getTop()));
        }
    }

    private void openPostAuthor(SocialStore.Post post, Character author) {
        if (post.authorId == SocialStore.MY_ID) { page = "profile"; render(); }
        else if (author != null) { selectedPerson = author; page = "person"; render(); }
    }

    private void renderSavedPosts() {
        addBackHeader("Publicações salvas", "Posts que você guardou para rever");
        int count = 0;
        for (SocialStore.Post post : social.data().posts) if (post.saved) { addPost(post); count++; }
        if (count == 0) empty("Nenhum post salvo", "Toque em Salvar em uma publicação para encontrá-la aqui.");
    }

    private void renderDrafts() {
        addBackHeader("Rascunhos", "Publicações guardadas neste aparelho");
        if (social.data().drafts.isEmpty()) {
            empty("Nenhum rascunho", "Salve uma publicação enquanto estiver criando para continuar depois.");
            addButton(pageBody, "Criar publicação", true, this::startNewPost); return;
        }
        for (SocialStore.Draft draft : new ArrayList<>(social.data().drafts)) {
            LinearLayout item = card(); item.setOrientation(LinearLayout.HORIZONTAL); item.setGravity(Gravity.CENTER_VERTICAL);
            if (draft.photoPath != null && !draft.photoPath.isEmpty()) {
                ImageView photo = new ImageView(this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                item.addView(photo, new LinearLayout.LayoutParams(dp(68), dp(68)));
                Glide.with(this).load(new File(draft.photoPath)).centerCrop().placeholder(R.drawable.bg_camera_image).error(R.drawable.bg_camera_image).into(photo);
            }
            LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(10), 0, dp(8), 0);
            String text = draft.caption == null || draft.caption.trim().isEmpty() ? "Publicação com foto" : draft.caption;
            TextView caption = addText(text, 13, true); caption.setMaxLines(2); caption.setEllipsize(android.text.TextUtils.TruncateAt.END); copy.addView(caption);
            copy.addView(addText("Salvo " + relative(draft.updatedAt), 11, false));
            item.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            addButton(item, "Editar", false, () -> openDraft(draft), dp(76));
            addButton(item, "Apagar", false, () -> new AlertDialog.Builder(this).setTitle("Apagar rascunho?")
                    .setMessage("O texto e a foto salvos serão removidos.").setNegativeButton("Cancelar", null)
                    .setPositiveButton("Apagar", (d, w) -> { social.deleteDraft(draft.id); render(); }).show(), dp(80));
            item.setContentDescription("Rascunho: " + text); pageBody.addView(item, verticalMargin(8));
        }
    }

    private void openDraft(SocialStore.Draft draft) {
        activeDraftId = draft.id; draftPhotoPath = draft.photoPath;
        pendingPhoto = null; pendingBitmap = null; page = "compose"; render();
    }

    private void renderSearch() {
        addBackHeader("Buscar", "Encontre personagens e publicações");
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Buscar por personagem ou palavra");
        input.setTextColor(Color.WHITE); input.setHintTextColor(color(R.color.text_secondary)); input.setBackgroundResource(R.drawable.bg_social_input);
        input.setPadding(dp(14), 0, dp(14), 0); input.setContentDescription("Buscar personagens e posts");
        pageBody.addView(input, new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); pageBody.addView(results);
        input.setText(peopleQuery); input.setSelection(input.length());
        Runnable update = () -> renderSearchResults(input.getText().toString().trim(), results);
        update.run();
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { peopleQuery = s.toString(); update.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderSearchResults(String query, LinearLayout target) {
        target.removeAllViews();
        if (query.isEmpty()) { emptyIn(target, "Comece sua busca", "Os resultados vão incluir personagens e posts."); return; }
        String term = query.toLowerCase(Locale.ROOT); int found = 0;
        List<Character> matches = new ArrayList<>();
        for (Character character : characters) if (character.name != null && character.name.toLowerCase(Locale.ROOT).contains(term)) matches.add(character);
        if (!matches.isEmpty()) {
            target.addView(addText("Personagens", 17, true));
            for (Character character : matches) { personRow(character, true, target); found++; if (found >= 8) break; }
        }
        List<SocialStore.Post> posts = new ArrayList<>();
        for (SocialStore.Post post : social.data().posts) {
            String authorName = post.authorName == null ? "" : post.authorName.toLowerCase(Locale.ROOT);
            String caption = post.caption == null ? "" : post.caption.toLowerCase(Locale.ROOT);
            if (authorName.contains(term) || caption.contains(term)) posts.add(post);
        }
        if (!posts.isEmpty()) {
            target.addView(addText("Publicações", 17, true));
            for (SocialStore.Post post : posts) {
                Character author = person(post.authorId); LinearLayout result = card(); result.setOrientation(LinearLayout.HORIZONTAL); result.setGravity(Gravity.CENTER_VERTICAL);
                result.addView(avatarView(post.authorId == SocialStore.MY_ID ? gameStore.progress().avatar : author, 48));
                LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(10), 0, 0, 0);
                copy.addView(addText(post.authorName, 13, true)); TextView caption = addText(post.caption, 12, false); caption.setMaxLines(2); caption.setEllipsize(android.text.TextUtils.TruncateAt.END); copy.addView(caption);
                result.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
                result.setClickable(true); result.setFocusable(true); result.setContentDescription("Abrir publicação de " + post.authorName);
                result.setOnClickListener(v -> { focusedPostId = post.id; feedMode = "Para você"; page = "feed"; render(); });
                target.addView(result, verticalMargin(6)); found++;
            }
        }
        if (found == 0) emptyIn(target, "Nenhum resultado", "Tente outro nome de personagem ou palavra.");
    }

    private void showPostOptions(SocialStore.Post post) {
        if (post.authorId != SocialStore.MY_ID) return;
        new AlertDialog.Builder(this).setTitle("Sua publicação")
                .setItems(new String[]{"Editar publicação", "Apagar publicação"}, (dialog, which) -> {
                    if (which == 0) editPost(post);
                    else confirmDeletePost(post);
                }).show();
    }

    private void editPost(SocialStore.Post post) {
        EditText input = new EditText(this); input.setText(post.caption == null ? "" : post.caption);
        input.setTextColor(Color.WHITE); input.setHintTextColor(color(R.color.text_secondary)); input.setHint("Escreva a legenda da publicação");
        input.setMinLines(3); input.setGravity(Gravity.TOP | Gravity.START); input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setBackgroundResource(R.drawable.bg_social_input);
        LinearLayout content = new LinearLayout(this); content.setPadding(dp(20), dp(8), dp(20), dp(4)); content.addView(input);
        new AlertDialog.Builder(this).setTitle("Editar publicação").setView(content)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    social.editPost(post, input.getText().toString().trim()); render();
                    Toast.makeText(this, "Publicação atualizada.", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void confirmDeletePost(SocialStore.Post post) {
        new AlertDialog.Builder(this).setTitle("Apagar publicação?")
                .setMessage("Essa publicação será removida do seu perfil e do feed.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (dialog, which) -> {
                    social.deletePost(post); render();
                    Toast.makeText(this, "Publicação apagada.", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void renderPeople() {
        heading("Amigos", "Encontre personagens para seguir");
        if (characters.isEmpty()) {
            empty(fetching ? "Carregando personagens…" : "Lista indisponível", fetching ? "Aguarde enquanto buscamos os perfis." : "Verifique sua conexão para atualizar a lista.");
            if (!fetching) addButton(pageBody, "Tentar novamente", false, this::loadCharacters);
            return;
        }
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Buscar personagens"); search.setTextColor(Color.WHITE);
        search.setHintTextColor(color(R.color.text_secondary)); search.setPadding(dp(14), 0, dp(14), 0); search.setBackgroundResource(R.drawable.bg_social_input);
        search.setText(peopleQuery); pageBody.addView(search, verticalMargin(10));
        LinearLayout results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); pageBody.addView(results);
        Runnable filter = () -> {
            results.removeAllViews(); String q = peopleQuery.toLowerCase(Locale.ROOT); int count = 0;
            for (Character c : characters) if (c != null && c.name != null && c.name.toLowerCase(Locale.ROOT).contains(q)) { personRow(c, true, results); count++; }
            if (count == 0) emptyIn(results, "Nenhum personagem encontrado", "Tente buscar por outro nome.");
        };
        filter.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { peopleQuery = s.toString(); filter.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void renderProfile() {
        Character avatar = gameStore.progress().avatar;
        heading("Meu perfil", "Seu espaço na comunidade");
        LinearLayout cover = card(); cover.setGravity(Gravity.CENTER_HORIZONTAL); cover.setPadding(dp(16), dp(18), dp(16), dp(16));
        ImageView portrait = avatarView(avatar, 92); cover.addView(portrait);
        TextView name = addText("Capitão América", 21, true); name.setGravity(Gravity.CENTER); cover.addView(name);
        TextView handle = addText("@capitaoamerica", 13, false); handle.setGravity(Gravity.CENTER); cover.addView(handle);
        TextView bio = addText("Herói, Vingador e defensor da liberdade.", 13, false); bio.setGravity(Gravity.CENTER); cover.addView(bio);
        addButton(cover, "Trocar foto de perfil", false, () -> { refreshOnResume = true; startActivity(new Intent(this, CharacterPickerActivity.class).putExtra(CharacterPickerActivity.EXTRA_AVATAR, true)); });
        LinearLayout counts = new LinearLayout(this); counts.setGravity(Gravity.CENTER); counts.setPadding(0, dp(12), 0, dp(6));
        countButton(counts, social.data().posts.stream().filter(p -> p.authorId == SocialStore.MY_ID).count() + "", "Publicações", () -> { page = "myPosts"; renderOwnPosts(); });
        countButton(counts, "" + social.followingFor(SocialStore.MY_ID).size(), "Seguindo", () -> { relationOwnerId = SocialStore.MY_ID; page = "following"; render(); });
        countButton(counts, "" + social.followersFor(SocialStore.MY_ID).size(), "Seguidores", () -> { relationOwnerId = SocialStore.MY_ID; page = "followers"; render(); }); cover.addView(counts); pageBody.addView(cover);
        button("＋  Criar publicação", true, this::startNewPost);
        addText("Minhas publicações", 19, true);
        List<SocialStore.Post> mine = new ArrayList<>(); for (SocialStore.Post p : social.data().posts) if (p.authorId == SocialStore.MY_ID) mine.add(p);
        if (mine.isEmpty()) empty("Você ainda não tem posts", "Quando publicar algo, suas publicações vão aparecer aqui.");
        else for (SocialStore.Post post : mine) addPost(post);
        page = "profile";
    }

    private void renderGames() {
        PlayerProgress progress = gameStore.progress();
        heading("Jogos", "Desafios para testar seus conhecimentos Marvel");
        LinearLayout overview = card();
        overview.addView(addText("Quiz Marvel", 20, true));
        overview.addView(addText("Responda às perguntas, some pontos e desbloqueie personagens para sua coleção.", 13, false));
        LinearLayout stats = new LinearLayout(this); stats.setGravity(Gravity.CENTER); stats.setPadding(0, dp(12), 0, dp(6));
        countButton(stats, String.valueOf(progress.points), "Pontos", () -> {});
        countButton(stats, String.valueOf(progress.blocks), "Partidas", () -> {});
        countButton(stats, String.valueOf(progress.correct), "Acertos", () -> {});
        overview.addView(stats);
        overview.addView(addText(progress.blocks == 0 ? "Sua primeira partida está pronta quando você estiver." : progress.collection.size() + " personagens desbloqueados na sua coleção.", 12, false));
        addButton(overview, "Começar desafio", true, () -> { refreshOnResume = true; startActivity(new Intent(this, QuizActivity.class)); });
        pageBody.addView(overview);
        addText("Personagens desbloqueados", 19, true);
        addText("Sua coleção organizada por raridade", 12, false);
        if (progress.collection.isEmpty()) {
            empty("Sua coleção está vazia", "Jogue uma partida para começar a desbloquear personagens.");
        } else for (String rarity : new String[]{"Lendário", "Épico", "Raro", "Comum"}) {
            List<PlayerProgress.Unlock> group = new ArrayList<>();
            for (PlayerProgress.Unlock unlock : progress.collection.values()) if (rarity.equals(QuizGame.rarity(unlock.bestPoints))) group.add(unlock);
            if (group.isEmpty()) continue;
            LinearLayout raritySection = card();
            TextView rarityTitle = addText(rarity, 15, true); rarityTitle.setTextColor(color(R.color.marvel_gold));
            raritySection.addView(rarityTitle);
            raritySection.addView(addText(group.size() + (group.size() == 1 ? " personagem" : " personagens"), 11, false));
            LinearLayout grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL); grid.setPadding(0, dp(8), 0, 0);
            for (int i = 0; i < group.size(); i += 2) {
                LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.TOP);
                addUnlockedCharacterCard(row, group.get(i));
                if (i + 1 < group.size()) addUnlockedCharacterCard(row, group.get(i + 1));
                else row.addView(new View(this), new LinearLayout.LayoutParams(0, dp(1), 1));
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2); rowParams.topMargin = dp(6); grid.addView(row, rowParams);
            }
            raritySection.addView(grid); pageBody.addView(raritySection, verticalMargin(9));
        }
    }

    private void addUnlockedCharacterCard(LinearLayout row, PlayerProgress.Unlock unlock) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(10), dp(8), dp(10));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(color(R.color.ink_3)); background.setCornerRadius(dp(13)); tile.setBackground(background);
        ImageView portrait = avatarView(unlock.character, 66); tile.addView(portrait);
        TextView name = addText(unlock.character.name, 12, true); name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END); name.setPadding(0, dp(6), 0, 0); tile.addView(name);
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -2, 1); tileParams.setMargins(dp(4), 0, dp(4), 0);
        row.addView(tile, tileParams);
    }

    private void renderOwnPosts() { page = "profile"; render(); }

    private void renderRelations(boolean followers) {
        boolean own = relationOwnerId == SocialStore.MY_ID;
        String owner = own ? "seu perfil" : selectedPerson == null ? "perfil" : selectedPerson.name;
        addBackHeader(followers ? "Seguidores" : "Seguindo", (followers ? "Pessoas que acompanham " : "Perfis acompanhados por ") + owner);
        List<Long> ids = followers ? social.followersFor(relationOwnerId) : social.followingFor(relationOwnerId);
        int shown = 0;
        for (Long id : ids) {
            if (id == SocialStore.MY_ID) { ownAccountRow(pageBody); shown++; }
            else { Character c = person(id); if (c != null) { personRow(c, !followers); shown++; } }
        }
        if (shown == 0) empty(followers ? "Ainda sem seguidores" : "Ainda não segue ninguém", "As conexões deste perfil vão aparecer aqui.");
    }

    private void ownAccountRow(LinearLayout target) {
        LinearLayout row = card(); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(avatarView(gameStore.progress().avatar, 50));
        LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(10), 0, 0, 0);
        copy.addView(addText("Capitão América", 14, true)); copy.addView(addText("@capitaoamerica", 11, false));
        row.addView(copy); row.setContentDescription("Abrir perfil de Capitão América");
        row.setClickable(true); row.setFocusable(true); row.setOnClickListener(v -> { page = "profile"; render(); });
        target.addView(row, verticalMargin(7));
    }

    private void renderPerson() {
        if (selectedPerson == null) { page = "people"; render(); return; }
        addBackHeader(selectedPerson.name, "Perfil de personagem · dados da API Marvel");
        LinearLayout profile = card(); profile.setGravity(Gravity.CENTER_HORIZONTAL); profile.setPadding(dp(16), dp(18), dp(16), dp(16));
        ImageView portrait = avatarView(selectedPerson, 96); profile.addView(portrait);
        TextView title = addText(selectedPerson.name, 21, true); title.setGravity(Gravity.CENTER); profile.addView(title);
        profile.addView(addText(selectedPerson.getShortDescription(), 13, false));
        if (selectedPerson.biography != null && selectedPerson.biography.alignment != null)
            profile.addView(addText("Alinhamento: " + selectedPerson.biography.alignment, 12, false));
        if (selectedPerson.connections != null && selectedPerson.connections.groupAffiliation != null)
            profile.addView(addText("Afiliações: " + selectedPerson.connections.groupAffiliation, 12, false));
        if (selectedPerson.work != null && selectedPerson.work.occupation != null)
            profile.addView(addText("Ocupação: " + selectedPerson.work.occupation, 12, false));
        LinearLayout stats = new LinearLayout(this); stats.setGravity(Gravity.CENTER); stats.setPadding(0, dp(12), 0, dp(4));
        long nPosts = social.data().posts.stream().filter(p -> p.authorId == selectedPerson.id).count();
        countButton(stats, "" + nPosts, "posts", () -> {});
        countButton(stats, "" + social.followingFor(selectedPerson.id).size(), "seguindo", () -> { relationOwnerId = selectedPerson.id; page = "following"; render(); });
        countButton(stats, "" + social.followersFor(selectedPerson.id).size(), "seguidores", () -> { relationOwnerId = selectedPerson.id; page = "followers"; render(); });
        profile.addView(stats);
        addButton(profile, social.follows(selectedPerson.id) ? "Seguindo" : "Seguir", !social.follows(selectedPerson.id), () -> { social.toggleFollow(selectedPerson); render(); });
        addButton(profile, "Mensagem", false, () -> { activeChat = selectedPerson; social.chat(selectedPerson); page = "chat"; render(); });
        pageBody.addView(profile);
        addText("Publicações", 19, true);
        int count = 0; for (SocialStore.Post p : social.data().posts) if (p.authorId == selectedPerson.id) { addPost(p); count++; }
        if (count == 0) empty("Ainda não há posts", selectedPerson.name + " ainda não publicou nada por aqui.");
    }

    private void renderChats() {
        heading("Chats", "Conversas da comunidade");
        if (social.data().chats.isEmpty()) { empty("Nenhuma conversa ainda", "Abra um perfil e toque em Mensagem para começar."); return; }
        for (SocialStore.Chat chat : social.data().chats) {
            Character c = person(chat.personId);
            LinearLayout row = card(); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(avatarView(c, 48));
            LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(12), 0, 0, 0);
            copy.addView(addText(chat.personName, 15, true));
            String last = chat.messages.isEmpty() ? "Comece uma conversa" : chat.messages.get(chat.messages.size() - 1).text;
            copy.addView(addText(last == null ? "Publicação compartilhada" : last, 12, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); row.setContentDescription("Conversa com " + chat.personName); row.setClickable(true); row.setFocusable(true);
            row.setOnClickListener(v -> { activeChat = c; page = "chat"; render(); }); pageBody.addView(row, verticalMargin(8));
        }
    }

    private void renderChat() {
        if (activeChat == null) { page = "chats"; render(); return; }
        addBackHeader(activeChat.name, "Conversa privada neste aparelho");
        SocialStore.Chat chat = social.chat(activeChat);
        LinearLayout messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); pageBody.addView(messages, new LinearLayout.LayoutParams(-1, -2));
        for (SocialStore.Message message : chat.messages) {
            LinearLayout bubble = new LinearLayout(this); bubble.setOrientation(LinearLayout.VERTICAL); bubble.setPadding(dp(12), dp(8), dp(12), dp(7));
            bubble.setBackgroundResource(message.mine ? R.drawable.bg_marvel_tag : R.drawable.bg_profile_card);
            bubble.addView(addText(message.mine ? "Você" : activeChat.name, 10, true));
            TextView body = addText(message.postId == null ? message.text : "Publicação compartilhada", 14, false); bubble.addView(body);
            TextView date = addText(relative(message.date), 10, false); bubble.addView(date);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * .72f), -2);
            bp.gravity = message.mine ? Gravity.END : Gravity.START; bp.topMargin = dp(7); messages.addView(bubble, bp);
            if (message.postId != null) { SocialStore.Post p = social.findPost(message.postId); if (p != null) body.setText(p.authorName + ": " + p.caption); }
        }
        LinearLayout compose = new LinearLayout(this); compose.setGravity(Gravity.CENTER_VERTICAL); compose.setPadding(0, dp(10), 0, 0);
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Escreva uma mensagem"); input.setTextColor(Color.WHITE); input.setHintTextColor(color(R.color.text_secondary));
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(R.color.marvel_red)));
        compose.addView(input, new LinearLayout.LayoutParams(0, dp(52), 1));
        addButton(compose, "Enviar", true, () -> { String value = input.getText().toString().trim(); if (value.isEmpty()) return; social.send(chat, value, null); render(); }, dp(78));
        pageBody.addView(compose);
    }

    private void renderNotifications() {
        heading("Notificações", "Novidades sobre seu perfil");
        if (social.data().notices.isEmpty()) { empty("Tudo em dia", "Quando alguém interagir com você, o aviso aparece aqui."); return; }
        for (SocialStore.Notice notice : social.data().notices) {
            LinearLayout row = card(); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
            TextView icon = addText("•", 28, true); icon.setTextColor(color(notice.unread ? R.color.marvel_red : R.color.text_muted)); icon.setPadding(0, 0, dp(12), 0); row.addView(icon);
            LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL);
            copy.addView(addText(notice.title, 14, true)); copy.addView(addText(notice.detail, 12, false)); row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            if (notice.postId != null) {
                row.setClickable(true); row.setFocusable(true); row.setContentDescription(notice.title + ". Abrir publicação.");
                row.setOnClickListener(v -> { social.markRead(); focusedPostId = notice.postId; page = "feed"; render(); });
            }
            pageBody.addView(row, verticalMargin(7));
        }
        social.markRead();
    }

    private void showLikers(SocialStore.Post post) {
        List<String> names = post.likedBy == null ? new ArrayList<>() : new ArrayList<>(post.likedBy);
        StringBuilder message = new StringBuilder();
        if (names.isEmpty()) message.append("Ainda não há nomes associados a essas curtidas.");
        else {
            for (String name : names) message.append("•  ").append(name).append('\n');
            int more = Math.max(0, post.likes - names.size());
            if (more > 0) message.append("\n+ ").append(more).append(" curtidas de exemplo");
        }
        new AlertDialog.Builder(this).setTitle("Curtidas").setMessage(message.toString()).setPositiveButton("Fechar", null).show();
    }

    private void renderComposer() {
        addBackHeader("Criar publicação", "Compartilhe algo com a comunidade");
        LinearLayout identity = card(); identity.setOrientation(LinearLayout.HORIZONTAL); identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.addView(avatarView(gameStore.progress().avatar, 46));
        LinearLayout who = new LinearLayout(this); who.setOrientation(LinearLayout.VERTICAL); who.setPadding(dp(10), 0, 0, 0);
        who.addView(addText("Capitão América", 15, true)); who.addView(addText("Publicação para todos", 12, false)); identity.addView(who); pageBody.addView(identity);
        postText = new EditText(this); postText.setMinLines(4); postText.setGravity(Gravity.TOP | Gravity.START); postText.setHint("O que está acontecendo no seu universo?");
        postText.setTextColor(Color.WHITE); postText.setHintTextColor(color(R.color.text_secondary)); postText.setTextSize(16);
        postText.setBackgroundResource(R.drawable.bg_social_input); postText.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (activeDraftId != null) for (SocialStore.Draft draft : social.data().drafts)
            if (Objects.equals(draft.id, activeDraftId)) { postText.setText(draft.caption); draftPhotoPath = draft.photoPath; break; }
        LinearLayout.LayoutParams inputSize = new LinearLayout.LayoutParams(-1, -2); inputSize.topMargin = dp(12); pageBody.addView(postText, inputSize);
        photoPreview = new ImageView(this); photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams previewSize = new LinearLayout.LayoutParams(-1, dp(220)); previewSize.topMargin = dp(10); pageBody.addView(photoPreview, previewSize);
        updatePhotoPreview();
        LinearLayout options = card(); options.setOrientation(LinearLayout.HORIZONTAL); options.setGravity(Gravity.CENTER_VERTICAL);
        options.addView(addText("Adicionar à publicação", 13, true), new LinearLayout.LayoutParams(0, -2, 1));
        addButton(options, "Câmera", false, () -> cameraPicker.launch(null), dp(82));
        addButton(options, "Fotos", false, () -> galleryPicker.launch("image/*"), dp(72)); pageBody.addView(options);
        addButton(pageBody, "Salvar rascunho", false, this::saveCurrentDraft);
        button("Publicar", true, this::publishPost);
    }

    private void updatePhotoPreview() {
        if (photoPreview == null) return;
        if (pendingBitmap != null) { photoPreview.setVisibility(View.VISIBLE); photoPreview.setImageBitmap(pendingBitmap); }
        else if (pendingPhoto != null) { photoPreview.setVisibility(View.VISIBLE); Glide.with(this).load(pendingPhoto).centerCrop().into(photoPreview); }
        else if (draftPhotoPath != null && !draftPhotoPath.isEmpty()) { photoPreview.setVisibility(View.VISIBLE); Glide.with(this).load(new File(draftPhotoPath)).centerCrop().placeholder(R.drawable.bg_camera_image).into(photoPreview); }
        else photoPreview.setVisibility(View.GONE);
    }

    private void startNewPost() {
        activeDraftId = null; draftPhotoPath = null; pendingPhoto = null; pendingBitmap = null; page = "compose"; render();
    }

    private void saveCurrentDraft() {
        if (postText == null) return;
        String caption = postText.getText().toString().trim();
        String path = pendingPhoto != null || pendingBitmap != null ? persistPhoto() : draftPhotoPath;
        if (caption.isEmpty() && path == null) { Toast.makeText(this, "Escreva algo ou adicione uma foto antes de salvar.", Toast.LENGTH_SHORT).show(); return; }
        social.saveDraft(activeDraftId, caption, path);
        activeDraftId = null; draftPhotoPath = null; pendingPhoto = null; pendingBitmap = null; page = "drafts"; render();
        Toast.makeText(this, "Rascunho salvo neste aparelho.", Toast.LENGTH_SHORT).show();
    }

    private void publishPost() {
        if (postText == null) return;
        String caption = postText.getText().toString().trim();
        if (caption.isEmpty() && pendingPhoto == null && pendingBitmap == null && draftPhotoPath == null) { Toast.makeText(this, "Escreva algo ou adicione uma foto.", Toast.LENGTH_SHORT).show(); return; }
        String path = pendingPhoto != null || pendingBitmap != null ? persistPhoto() : draftPhotoPath;
        social.createPost(caption, path, gameStore.progress().avatar);
        if (activeDraftId != null) social.deleteDraft(activeDraftId);
        activeDraftId = null; draftPhotoPath = null;
        pendingPhoto = null; pendingBitmap = null; page = "feed"; render();
        Toast.makeText(this, "Publicação compartilhada no seu feed.", Toast.LENGTH_SHORT).show();
    }

    private String persistPhoto() {
        File folder = new File(getFilesDir(), "social_posts"); if (!folder.exists() && !folder.mkdirs()) return null;
        File file = new File(folder, "post-" + System.currentTimeMillis() + ".jpg");
        try (OutputStream output = new FileOutputStream(file)) {
            Bitmap bitmap = pendingBitmap;
            if (bitmap != null) return bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output) ? file.getAbsolutePath() : null;
            if (pendingPhoto == null) return null;
            try (InputStream input = getContentResolver().openInputStream(pendingPhoto)) {
                if (input == null) return null;
                byte[] buffer = new byte[8192]; int read; while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
            return file.getAbsolutePath();
        } catch (IOException e) { Toast.makeText(this, "Não consegui salvar essa imagem.", Toast.LENGTH_SHORT).show(); return null; }
    }

    private int commentCount(List<SocialStore.Comment> comments) {
        int count = 0;
        if (comments != null) for (SocialStore.Comment comment : comments) {
            count++;
            count += commentCount(comment.replies);
        }
        return count;
    }

    private void addCommentThread(LinearLayout target, SocialStore.Post post, SocialStore.Comment comment, int depth, Runnable onReply, Runnable onChanged) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.TOP); row.setPadding(dp(8 + depth * 24), dp(10), dp(8), dp(5));
        row.addView(avatarView("Capitão América".equals(comment.author) ? gameStore.progress().avatar : null, 36));
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(10), 0, 0, 0);
        TextView text = addText(comment.author + "  " + comment.text, 13, false); content.addView(text);
        TextView reply = addText("Responder", 12, true); reply.setTextColor(color(R.color.text_secondary));
        reply.setPadding(0, dp(6), 0, dp(2)); reply.setMinimumHeight(dp(44)); reply.setContentDescription("Responder a " + comment.author);
        reply.setClickable(true); reply.setFocusable(true); reply.setOnClickListener(v -> { replyingTo = comment; onReply.run(); }); content.addView(reply);
        row.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
        TextView like = addText((comment.liked ? "♥" : "♡") + (comment.likes > 0 ? " " + comment.likes : ""), 15, true);
        like.setTextColor(color(comment.liked ? R.color.marvel_red : R.color.text_secondary)); like.setGravity(Gravity.CENTER);
        like.setMinWidth(dp(48)); like.setMinHeight(dp(48)); like.setClickable(true); like.setFocusable(true);
        like.setContentDescription(comment.liked ? "Descurtir comentário" : "Curtir comentário");
        like.setOnClickListener(v -> { social.toggleLike(comment); onChanged.run(); }); row.addView(like);
        target.addView(row);
        if (comment.replies != null) for (SocialStore.Comment child : comment.replies) addCommentThread(target, post, child, depth + 1, onReply, onChanged);
    }

    private SocialStore.Comment replyingTo;

    private void showComments(SocialStore.Post post) {
        replyingTo = null;
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(12), dp(8), dp(12), dp(8));
        android.graphics.drawable.GradientDrawable sheetBackground = new android.graphics.drawable.GradientDrawable();
        sheetBackground.setColor(color(R.color.ink_2));
        float radius = dp(26);
        sheetBackground.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        body.setBackground(sheetBackground);
        View handle = new View(this); android.graphics.drawable.GradientDrawable handleBackground = new android.graphics.drawable.GradientDrawable();
        handleBackground.setColor(color(R.color.text_secondary)); handleBackground.setCornerRadius(dp(8)); handle.setBackground(handleBackground);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(5));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = dp(2); handleParams.bottomMargin = dp(12); body.addView(handle, handleParams);
        TextView title = addText("Comentários", 18, true); title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, dp(42)); body.addView(title, titleParams);
        View divider = new View(this); divider.setBackgroundColor(color(R.color.divider));
        body.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Adicione um comentário..."); input.setTextColor(Color.WHITE); input.setHintTextColor(color(R.color.text_secondary));
        ScrollView comments = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        final Runnable[] rebuildRef = new Runnable[1];
        Runnable rebuild = () -> { list.removeAllViews(); if (post.comments != null) for (SocialStore.Comment c : post.comments) addCommentThread(list, post, c, 0, () -> input.setHint("Respondendo a " + replyingTo.author + "…"), () -> { if (rebuildRef[0] != null) rebuildRef[0].run(); }); };
        rebuildRef[0] = rebuild;
        rebuild.run(); comments.addView(list);
        body.addView(comments, new LinearLayout.LayoutParams(-1, 0, 1));
        View composerDivider = new View(this); composerDivider.setBackgroundColor(color(R.color.divider));
        body.addView(composerDivider, new LinearLayout.LayoutParams(-1, dp(1)));
        LinearLayout composer = new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setPadding(dp(2), dp(3), dp(2), 0);
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(52), 1));
        TextView send = addText("Publicar", 14, true); send.setTextColor(color(R.color.marvel_red)); send.setPadding(dp(12), dp(12), dp(8), dp(12)); composer.addView(send); body.addView(composer);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        dialog.setContentView(body);
        dialog.setOnShowListener(ignored -> {
            android.view.Window window = dialog.getWindow();
            if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int)(getResources().getDisplayMetrics().heightPixels * .76f));
            View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.getLayoutParams().height = (int)(getResources().getDisplayMetrics().heightPixels * .76f);
                sheet.requestLayout();
                sheet.setBackgroundColor(Color.TRANSPARENT);
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });
        send.setOnClickListener(v -> {
            String value = input.getText().toString().trim(); if (value.isEmpty()) return;
            if (replyingTo == null) social.comment(post, value); else social.reply(post, replyingTo, value);
            replyingTo = null; input.setText(""); input.setHint("Adicione um comentário..."); rebuild.run(); render();
        });
        dialog.show();
    }

    private void sharePost(SocialStore.Post post) {
        sharingPost = post;
        List<Character> recipients = new ArrayList<>(); for (SocialStore.Chat chat : social.data().chats) { Character c = person(chat.personId); if (c != null) recipients.add(c); }
        if (recipients.isEmpty()) { Toast.makeText(this, "Inicie uma conversa pelo perfil da pessoa antes de enviar.", Toast.LENGTH_LONG).show(); return; }
        String[] names = new String[recipients.size()]; for (int i = 0; i < names.length; i++) names[i] = recipients.get(i).name;
        new AlertDialog.Builder(this).setTitle("Enviar para").setItems(names, (d, which) -> {
            Character recipient = recipients.get(which); SocialStore.Chat chat = social.chat(recipient);
            social.send(chat, "Publicação de " + post.authorName, post.id);
            Toast.makeText(this, "Enviado para " + recipient.name, Toast.LENGTH_SHORT).show();
        }).show();
    }

    private void personRow(Character c, boolean showFollow) {
        personRow(c, showFollow, pageBody);
    }

    private void personRow(Character c, boolean showFollow, LinearLayout target) {
        LinearLayout row = card(); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(avatarView(c, 50));
        LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL); labels.setPadding(dp(10), 0, dp(6), 0);
        labels.addView(addText(c.name, 14, true));
        long count = social.data().posts.stream().filter(p -> p.authorId == c.id).count();
        labels.addView(addText(count == 0 ? "Ainda sem posts" : count + (count == 1 ? " publicação" : " publicações"), 11, false));
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        if (showFollow) addButton(row, social.follows(c.id) ? "Seguindo" : "Seguir", !social.follows(c.id), () -> { social.toggleFollow(c); render(); }, dp(102));
        row.setOnClickListener(v -> { selectedPerson = c; page = "person"; render(); }); target.addView(row, verticalMargin(7));
    }

    private void countButton(LinearLayout parent, String count, String label, Runnable action) {
        LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(dp(12), dp(6), dp(12), dp(6));
        item.addView(addText(count, 18, true)); item.addView(addText(label, 11, false)); item.setClickable(true); item.setFocusable(true);
        item.setMinimumHeight(dp(48)); item.setContentDescription(count + " " + label); item.setOnClickListener(v -> action.run());
        parent.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void shareMessage(long characterId) { }
    private void openSidebar() {
        if (sidebarOverlay != null) return;
        FrameLayout overlay = new FrameLayout(this); overlay.setBackgroundColor(0x99000000); overlay.setClickable(true);
        LinearLayout drawer = new LinearLayout(this); drawer.setOrientation(LinearLayout.VERTICAL); drawer.setBackgroundColor(color(R.color.ink_2));
        drawer.setPadding(dp(18), dp(24), dp(18), dp(20)); drawer.setOnClickListener(v -> {});
        int width = Math.min(dp(320), (int)(getResources().getDisplayMetrics().widthPixels * .84f));
        overlay.addView(drawer, new FrameLayout.LayoutParams(width, -1, Gravity.START));
        TextView close = addText("MARVELVERSE                       ×", 19, true); close.setPadding(0, dp(12), 0, dp(22));
        close.setOnClickListener(v -> closeSidebar()); drawer.addView(close);
        addDrawerItem(drawer, "◉", "Meu perfil", () -> navigateDrawer("profile"));
        addDrawerItem(drawer, "⌕", "Buscar", () -> navigateDrawer("search"));
        addDrawerItem(drawer, "▣", "Publicações salvas", () -> navigateDrawer("saved"));
        addDrawerItem(drawer, "▧", "Rascunhos", () -> navigateDrawer("drafts"));
        addDrawerItem(drawer, "♧", "Amigos e seguidores", () -> navigateDrawer("people"));
        addDrawerItem(drawer, "▤", "Chats", () -> navigateDrawer("chats"));
        addDrawerItem(drawer, "♢", "Notificações", () -> navigateDrawer("notifications"));
        View divider = new View(this); divider.setBackgroundColor(color(R.color.divider)); LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1)); line.topMargin = dp(12); line.bottomMargin = dp(12); drawer.addView(divider, line);
        addDrawerItem(drawer, "✦", "Jogos", () -> navigateDrawer("games"));
        TextView gameHint = addText("Veja seu progresso e personagens desbloqueados na aba Jogos.", 12, false);
        gameHint.setPadding(0, dp(12), 0, 0); drawer.addView(gameHint);
        overlay.setOnClickListener(v -> closeSidebar());
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1)); sidebarOverlay = overlay;
    }

    private void addDrawerItem(LinearLayout drawer, String icon, String label, Runnable action) {
        LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(10), dp(12), dp(8), dp(12));
        TextView symbol = addText(icon, 19, true); symbol.setTextColor(color(R.color.marvel_gold)); item.addView(symbol, new LinearLayout.LayoutParams(dp(40), -2));
        item.addView(addText(label, 15, true)); item.setClickable(true); item.setFocusable(true); item.setOnClickListener(v -> action.run());
        drawer.addView(item, new LinearLayout.LayoutParams(-1, -2));
    }

    private void navigateDrawer(String destination) {
        closeSidebar(); page = destination; if (destination.equals("notifications")) social.markRead(); render();
    }

    private void closeSidebar() {
        if (sidebarOverlay != null) { root.removeView(sidebarOverlay); sidebarOverlay = null; }
    }

    private void loadCharacters() {
        if (fetching) return;
        fetching = true; render();
        request = MarvelApiService.getApi().getAll();
        request.enqueue(new Callback<List<Character>>() {
            @Override public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                if (isFinishing() || isDestroyed()) return; fetching = false;
                if (!response.isSuccessful() || response.body() == null) { render(); Toast.makeText(MainActivity.this, "Não foi possível carregar os personagens. Tente novamente.", Toast.LENGTH_SHORT).show(); return; }
                List<Character> marvel = new ArrayList<>();
                for (Character c : response.body()) if (c.biography != null && c.biography.publisher != null
                        && c.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) marvel.add(c);
                characters.clear(); characters.addAll(marvel); gameStore.cache(marvel); social.seed(marvel);
                if (!page.equals("compose") && !page.equals("chat")) render();
            }
            @Override public void onFailure(Call<List<Character>> call, Throwable t) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return; fetching = false; render();
                Toast.makeText(MainActivity.this, "Sem conexão para carregar os personagens.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshFeed() {
        if (refreshingFeed || fetching) return;
        if (characters.isEmpty()) {
            Toast.makeText(this, "Carregando personagens para o feed…", Toast.LENGTH_SHORT).show();
            loadCharacters();
            return;
        }
        refreshingFeed = true;
        render();
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            int added = social.addCommunityPosts(characters);
            refreshingFeed = false;
            render();
            Toast.makeText(this, added == 0 ? "Não há novos posts de exemplo agora." : added + " novas publicações no feed", Toast.LENGTH_SHORT).show();
        }, 450);
    }

    private void addBackHeader(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = new ImageButton(this); back.setImageResource(R.drawable.ic_arrow_back); back.setPadding(dp(12), dp(12), dp(12), dp(12));
        android.util.TypedValue ripple = new android.util.TypedValue(); getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true);
        back.setBackgroundResource(ripple.resourceId); back.setContentDescription("Voltar");
        row.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52))); back.setOnClickListener(v -> {
            if (page.equals("person")) page = "people";
            else if (page.equals("followers") || page.equals("following")) page = relationOwnerId == SocialStore.MY_ID ? "profile" : "person";
            else if (page.equals("saved") || page.equals("drafts") || page.equals("search")) page = "feed";
            else page = "profile";
            render();
        });
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.addView(addText(title, 21, true)); titles.addView(addText(subtitle, 12, false));
        row.addView(titles); pageBody.addView(row, verticalMargin(5));
    }

    private void heading(String title, String subtitle) { addText(title, 23, true); addText(subtitle, 13, false); }
    private TextView addText(String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value == null ? "" : value); view.setTextSize(size);
        view.setTextColor(color(bold ? R.color.text_primary : R.color.text_secondary)); if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(4)); return view;
    }
    private void addInfo(String value, boolean error) {
        LinearLayout box = card(); TextView info = addText(value, 13, false); box.addView(info);
        if (error) box.setOnClickListener(v -> loadCharacters()); pageBody.addView(box, verticalMargin(8));
    }
    private void empty(String title, String message) {
        emptyIn(pageBody, title, message);
    }
    private void emptyIn(LinearLayout target, String title, String message) {
        LinearLayout box = card(); box.setGravity(Gravity.CENTER); box.setPadding(dp(20), dp(24), dp(20), dp(24));
        TextView mark = addText("✦", 25, true); mark.setTextColor(color(R.color.marvel_gold)); mark.setGravity(Gravity.CENTER); box.addView(mark);
        TextView heading = addText(title, 16, true); heading.setGravity(Gravity.CENTER); box.addView(heading);
        TextView detail = addText(message, 13, false); detail.setGravity(Gravity.CENTER); box.addView(detail); target.addView(box, verticalMargin(14));
    }
    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14), dp(11), dp(14), dp(11));
        box.setBackgroundResource(R.drawable.bg_profile_card); return box;
    }
    private void button(String label, boolean primary, Runnable action) { addButton(pageBody, label, primary, action); }
    private void addButton(LinearLayout parent, String label, boolean primary, Runnable action) {
        MaterialButton b = new MaterialButton(this); b.setText(label); b.setAllCaps(false); b.setMinHeight(dp(46)); b.setCornerRadius(dp(12));
        b.setContentDescription(label);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(primary ? R.color.marvel_red : R.color.ink_3)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(7); parent.addView(b, lp); b.setOnClickListener(v -> action.run());
    }
    private void addButton(LinearLayout parent, String label, boolean primary, Runnable action, int width) {
        MaterialButton b = new MaterialButton(this); b.setText(label); b.setAllCaps(false); b.setMinHeight(dp(48)); b.setCornerRadius(dp(12));
        b.setContentDescription(label);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(primary ? R.color.marvel_red : R.color.ink_3)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, -2); lp.setMarginStart(dp(5)); parent.addView(b, lp); b.setOnClickListener(v -> action.run());
    }
    private void action(LinearLayout parent, String label, Runnable click, boolean selected) {
        TextView b = new TextView(this); b.setText(label); b.setTextSize(12); b.setGravity(Gravity.CENTER); b.setPadding(dp(4), dp(10), dp(4), dp(10));
        b.setTypeface(null, Typeface.BOLD); b.setTextColor(color(selected ? R.color.marvel_red : R.color.text_secondary)); b.setClickable(true); b.setFocusable(true); b.setMinHeight(dp(48));
        b.setContentDescription(label.replace("♡", "Curtir").replace("♥", "Curtido").replace("▢", "").replace("↗", "Compartilhar").trim());
        b.setOnClickListener(v -> click.run());
        parent.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void saveAction(LinearLayout parent, boolean selected, Runnable click) {
        LinearLayout item = new LinearLayout(this); item.setGravity(Gravity.CENTER); item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(dp(4), dp(10), dp(4), dp(10)); item.setMinimumHeight(dp(48)); item.setClickable(true); item.setFocusable(true);
        ImageView icon = new ImageView(this); icon.setImageResource(selected ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
        icon.setColorFilter(color(selected ? R.color.marvel_red : R.color.text_secondary));
        item.addView(icon, new LinearLayout.LayoutParams(dp(19), dp(19)));
        TextView label = addText(selected ? "Salvo" : "Salvar", 12, true);
        label.setTextColor(color(selected ? R.color.marvel_red : R.color.text_secondary)); label.setPadding(dp(4), 0, 0, 0); item.addView(label);
        item.setContentDescription(selected ? "Remover publicação dos salvos" : "Salvar publicação"); item.setOnClickListener(v -> click.run());
        parent.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private LinearLayout.LayoutParams verticalMargin(int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(top); return lp; }
    private ImageView avatarView(Character character, int size) {
        ImageView image = new ImageView(this); image.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size))); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.bg_social_icon); image.setClipToOutline(true);
        image.setContentDescription(character == null ? "Foto de perfil" : "Foto de " + character.name);
        if (character != null && character.getImageUrl() != null) Glide.with(this).load(character.getImageUrl()).circleCrop().placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(image);
        else image.setImageResource(R.drawable.ic_profile);
        return image;
    }
    private Character person(long id) { for (Character c : characters) if (c.id == id) return c; return null; }
    private boolean isMarvel(Character c) { return c != null && c.name != null && c.biography != null && c.biography.publisher != null && c.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel"); }
    private String relative(long time) {
        if (time <= 0) return "agora"; long minutes = Math.max(0, (System.currentTimeMillis() - time) / 60000);
        if (minutes < 1) return "agora"; if (minutes < 60) return "há " + minutes + " min";
        long hours = minutes / 60; if (hours < 24) return "há " + hours + " h"; return DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(time));
    }
    private int color(int resource) { return getColor(resource); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() { super.onResume(); if (root != null && refreshOnResume && !fetching) { refreshOnResume = false; render(); } }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if ("profile".equals(intent.getStringExtra("open_page"))) page = "profile";
        if (root != null) render();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("social_page", page); state.putString("people_query", peopleQuery); super.onSaveInstanceState(state); }
    @Override public void onBackPressed() {
        if (sidebarOverlay != null) closeSidebar();
        else if (page.equals("feed")) super.onBackPressed();
        else if (page.equals("chat")) { page = "chats"; render(); }
        else if (page.equals("person")) { page = "people"; render(); }
        else if (page.equals("followers") || page.equals("following")) { page = relationOwnerId == SocialStore.MY_ID ? "profile" : "person"; render(); }
        else { page = "feed"; render(); }
    }
    @Override protected void onDestroy() { if (request != null) request.cancel(); if (pendingBitmap != null && !pendingBitmap.isRecycled()) pendingBitmap.recycle(); super.onDestroy(); }
}
