package com.example.marvelcerto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.example.marvelcerto.api.Character;
import com.example.marvelcerto.api.MarvelApiService;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import retrofit2.*;

public class QuizActivity extends GameScreen {
    private GameStore store;
    private QuizGame game;
    private List<Character> catalog = new ArrayList<>();
    private Call<List<Character>> request;
    private boolean loading;
    private String category = "Misturado";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); createScreen(false);
        store = new GameStore(this); catalog = store.catalog(); game = store.session();
        if (state != null) category = state.getString("category", "Misturado");
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leave(); }
        });
        if (game != null) render(); else intro(null);
    }
    private void leave() {
        if (game != null && !game.result && !game.complete()) {
            new AlertDialog.Builder(this).setTitle("Pausar desafio?")
                    .setMessage("Você poderá continuar desta pergunta ao abrir o jogo novamente.")
                    .setNegativeButton("Continuar jogando", null).setPositiveButton("Sair", (d, w) -> finish()).show();
        } else finish();
    }
    private void header(String title) {
        content.removeAllViews();
        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_arrow_back);
        back.setPadding(dp(12), dp(12), dp(12), dp(12));
        back.setScaleType(ImageView.ScaleType.FIT_CENTER);
        android.util.TypedValue rippleBackground = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, rippleBackground, true);
        back.setBackgroundResource(rippleBackground.resourceId);
        back.setContentDescription("Voltar");
        content.addView(back, new LinearLayout.LayoutParams(dp(56), dp(56)));
        back.setOnClickListener(v -> leave());
        text(content, title, 26, true);
    }
    private void intro(String error) {
        header("Quem é o personagem?");
        text(content, "6 perguntas por bloco. Sem cronômetro.", 16, false);
        LinearLayout info = card(content);
        text(info, "Como jogar", 17, true);
        text(info, "Escolha uma das quatro respostas. Cada pista extra reduz os pontos da pergunta. Você verá se acertou antes de continuar.", 14, false);
        text(info, "Cada acerto desbloqueia o personagem. A raridade da sua carta depende das pistas usadas: 100 pts · Lendário; 75 · Épico; 50 · Raro; 25 · Comum. Repetir um acerto pode melhorar a carta.", 14, false);
        text(content, "Categoria", 16, true);
        RadioGroup categories = new RadioGroup(this);
        categories.setOrientation(RadioGroup.VERTICAL);
        for (String value : new String[]{"Heróis", "Vilões", "Misturado"}) {
            RadioButton option = new RadioButton(this); option.setId(View.generateViewId()); option.setText(value);
            option.setMinHeight(dp(48)); option.setTextColor(getColor(R.color.text_primary));
            categories.addView(option); option.setChecked(category.equals(value));
            option.setOnClickListener(v -> category = value);
        }
        content.addView(categories);
        if (error != null) text(content, error, 14, false).setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        MaterialButton start = button(content, loading ? "Carregando…" : "Começar desafio", true, () -> startBlock(null));
        start.setEnabled(!loading);
    }
    private void startBlock(QuizGame previous) {
        if (loading) return;
        if (catalog.isEmpty()) { loadCatalog(previous); return; }
        try {
            game = store.beginBlock(catalog, previous == null ? category : previous.category, previous);
            render();
        } catch (IllegalArgumentException e) {
            if (previous == null) intro(e.getMessage());
            else new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton("OK", null).show();
        }
    }
    private void loadCatalog(QuizGame previous) {
        loading = true;
        if (previous == null) intro(null);
        else { header("Preparando mais 6 perguntas"); text(content, "Carregando personagens…", 16, false); }
        request = MarvelApiService.getApi().getAll();
        request.enqueue(new Callback<List<Character>>() {
            @Override public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                if (isDestroyed() || isFinishing()) return;
                loading = false;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    catalog = new ArrayList<>();
                    for (Character character : response.body()) if (character != null && character.biography != null
                            && character.biography.publisher != null && character.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) catalog.add(character);
                    if (!catalog.isEmpty()) { store.cache(catalog); startBlock(previous); } else failed(previous);
                } else failed(previous);
            }
            @Override public void onFailure(Call<List<Character>> call, Throwable t) {
                if (call.isCanceled() || isDestroyed() || isFinishing()) return;
                loading = false; failed(previous);
            }
        });
    }
    private void failed(QuizGame previous) {
        if (previous == null) intro("Não foi possível carregar. Verifique sua conexão e tente começar novamente.");
        else { render(); Toast.makeText(this, "Sem conexão. Seus pontos estão salvos; tente novamente.", Toast.LENGTH_LONG).show(); }
    }
    private void render() {
        if (game.result) { result(); return; }
        if (game.complete() && game.index == 6) { checkpoint(); return; }
        QuizGame.Question q = game.current();
        header("Quem é o personagem?");
        text(content, "Pergunta " + (game.index + 1) + " de 6  ·  " + game.totalPoints() + " pontos", 15, false);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(6); progress.setProgress(game.index + (q.answered() ? 1 : 0)); content.addView(progress);
        progress.setContentDescription("Pergunta " + (game.index + 1) + " de 6");
        LinearLayout clue = card(content);
        text(clue, q.answered() ? "Resposta revelada" : "Pista " + (q.hint + 1) + " · vale " + q.value() + " pontos", 16, true);
        if (q.hint >= 2 || q.answered()) {
            ImageView image = new ImageView(this);
            boolean cropped = q.hint == 2 && !q.answered();
            image.setScaleType(cropped ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            image.setContentDescription(q.answered() ? q.target.name : "Pista visual do personagem misterioso");
            clue.addView(image, new LinearLayout.LayoutParams(-1, dp(cropped ? 72 : 200)));
            Glide.with(this).load(q.target.getImageUrl()).error(android.R.drawable.ic_menu_gallery).into(image);
        } else text(clue, "?", 48, true).setGravity(android.view.Gravity.CENTER);
        text(clue, q.clue(), 16, false);
        for (Character option : q.choices) {
            String label = option.name;
            if (q.answered() && option.id == q.target.id) label += " · correta";
            else if (q.answered() && option.id == q.selectedId) label += " · sua resposta";
            MaterialButton answer = button(content, label, false, () -> { game.answer(option.id); store.save(game); render(); });
            answer.setEnabled(!q.answered());
        }
        if (!q.answered()) {
            MaterialButton hint = button(content, q.hint == 3 ? "Todas as pistas reveladas" : "Mais uma pista · vale " + (q.value() - 25) + " pts", false,
                    () -> { game.moreHint(); store.save(game); render(); });
            hint.setEnabled(q.hint < 3);
        } else {
            TextView feedback = text(content, q.correct() ? "Resposta correta! " + q.target.name + ". Você ganhou +" + q.points + " pontos. Carta " + QuizGame.rarity(q.points) + "."
                    : "Resposta incorreta. A resposta certa é " + q.target.name + ". Você ganhou 0 pontos.", 17, true);
            feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            feedback.setFocusable(true);
            feedback.setContentDescription(feedback.getText());
            if (!q.correct()) {
                LinearLayout curiosity = card(content);
                text(curiosity, "Por que " + q.target.name + " é a resposta?", 15, true);
                text(curiosity, q.explanation(), 14, false);
                text(curiosity, q.curiosity(), 13, false);
            }
            button(content, game.index == 5 ? "Concluir as 6 perguntas" : "Próxima pergunta", true, () -> {
                game.index++; store.save(game); render();
            });
        }
    }
    private void checkpoint() {
        store.credit(game);
        header("Bloco concluído");
        text(content, "Você acertou " + game.blockCorrect() + " de 6 perguntas.", 20, true);
        text(content, "+" + game.blockPoints() + " pontos neste bloco. Pontos e cartas já salvos no perfil.", 16, false);
        text(content, "Quer continuar?", 20, true);
        button(content, "Jogar mais 6 perguntas", true, () -> startBlock(game));
        button(content, "Ver minha pontuação", false, () -> { game.result = true; store.save(game); result(); });
    }
    private void result() {
        store.credit(game);
        header("Seu resultado");
        LinearLayout summary = card(content);
        text(summary, game.totalPoints() + " pontos", 32, true).setTextColor(getColor(R.color.marvel_gold));
        text(summary, game.totalCorrect() + " acertos em " + (game.previousQuestions + 6) + " perguntas", 17, false);
        text(content, "Os personagens acertados já estão na sua coleção. Cartas repetidas mantêm a melhor raridade conquistada.", 15, false);
        button(content, "Ver meu perfil", true, () -> {
            Intent profile = new Intent(this, MainActivity.class).putExtra("open_page", "profile")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(profile); finish();
        });
        button(content, "Jogar novamente", false, () -> { store.clearSession(); game = null; intro(null); });
        button(content, "Compartilhar resultado", false, () -> {
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                    "Fiz " + game.totalPoints() + " pontos e acertei " + game.totalCorrect() + " de " + (game.previousQuestions + 6) + " personagens no MarvelVerse. Você consegue superar?");
            startActivity(Intent.createChooser(share, "Compartilhar resultado"));
        });
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("category", category); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { if (request != null) request.cancel(); super.onDestroy(); }
}
