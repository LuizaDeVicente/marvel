package com.example.marvelcerto;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.example.marvelcerto.api.Character;
import java.text.DateFormat;
import java.util.*;

public class PlayerProfileActivity extends GameScreen {
    private GameStore store;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); store = new GameStore(this); createScreen(true);
    }
    @Override protected void onResume() { super.onResume(); render(); }
    private void render() {
        content.removeAllViews();
        PlayerProgress progress = store.progress();
        text(content, "Meu perfil", 28, true);
        text(content, "Seu progresso neste celular", 14, false);
        LinearLayout identity = card(content);
        ImageView avatar = new ImageView(this);
        LinearLayout.LayoutParams imageSize = new LinearLayout.LayoutParams(dp(88), dp(88));
        imageSize.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        identity.addView(avatar, imageSize);
        avatar.setContentDescription("Foto de perfil");
        if (progress.avatar != null) Glide.with(this).load(progress.avatar.getImageUrl()).circleCrop()
                .placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(avatar);
        else avatar.setImageResource(R.drawable.ic_profile);
        text(identity, progress.avatar == null ? "Escolha seu personagem" : progress.avatar.name, 18, true)
                .setGravity(android.view.Gravity.CENTER);
        button(identity, "Trocar foto de perfil", false, () -> startActivity(new Intent(this, CharacterPickerActivity.class)
                .putExtra(CharacterPickerActivity.EXTRA_AVATAR, true)));
        LinearLayout stats = card(content);
        text(stats, progress.points + " pontos", 28, true).setTextColor(getColor(R.color.marvel_gold));
        text(stats, progress.blocks + " blocos concluídos · " + progress.correct + " acertos\n" + progress.collection.size() + " personagens desbloqueados", 15, false);
        text(content, "Minha coleção", 21, true);
        text(content, "Cada acerto desbloqueia uma carta. Menos pistas = raridade maior. Um novo acerto pode melhorar uma carta que você já tem.", 14, false);
        if (progress.collection.isEmpty()) text(card(content), "Sua coleção começa no primeiro bloco concluído. Toque no botão flutuante para jogar.", 16, false);
        for (String rarity : new String[]{"Lendário", "Épico", "Raro", "Comum"}) {
            List<PlayerProgress.Unlock> group = new ArrayList<>();
            for (PlayerProgress.Unlock unlock : progress.collection.values()) if (rarity.equals(QuizGame.rarity(unlock.bestPoints))) group.add(unlock);
            if (group.isEmpty()) continue;
            group.sort(Comparator.comparing(u -> u.character.name));
            text(content, rarity + " · " + group.size(), 17, true);
            for (PlayerProgress.Unlock unlock : group) {
                LinearLayout row = card(content); row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                ImageView portrait = new ImageView(this); portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(56), dp(64)); size.setMarginEnd(dp(12));
                row.addView(portrait, size);
                portrait.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                Glide.with(this).load(unlock.character.getImageUrl()).error(R.drawable.ic_profile).into(portrait);
                LinearLayout labels = new LinearLayout(this); labels.setOrientation(LinearLayout.VERTICAL);
                row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                text(labels, unlock.character.name, 16, true); text(labels, rarity + " · " + unlock.bestPoints + " pts", 13, false);
                row.setFocusable(true); row.setContentDescription("Ver " + unlock.character.name + ", carta " + rarity);
                row.setOnClickListener(v -> startActivity(new Intent(this, CharacterPerfilActivity.class)
                        .putExtra(CharacterPerfilActivity.EXTRA_CHARACTER_ID, unlock.character.id)));
            }
        }
        text(content, "Últimas partidas", 21, true);
        if (progress.history.isEmpty()) text(content, "Nenhum bloco concluído ainda.", 15, false);
        DateFormat dates = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (PlayerProgress.Match match : progress.history) {
            LinearLayout row = card(content);
            text(row, "+" + match.points + " pontos · " + match.correct + "/6 acertos", 17, true);
            text(row, match.category + " · " + dates.format(new Date(match.date)), 13, false);
        }
    }
}
