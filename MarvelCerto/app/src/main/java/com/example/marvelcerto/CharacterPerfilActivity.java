package com.example.marvelcerto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.marvelcerto.api.Character;
import com.example.marvelcerto.api.MarvelApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CharacterPerfilActivity extends AppCompatActivity {

    public static final String EXTRA_CHARACTER_ID = "CHARACTER_ID";

    private ImageView imgHero;
    private TextView txtNomePersonagem;
    private TextView txtNomeReal;
    private TextView txtBiografia;
    private TextView txtEditora;
    private TextView txtAlinhamento;
    private TextView txtAparencia;
    private TextView txtOcupacao;
    private TextView txtAfiliacao;
    private TextView txtEstatisticas;
    private ImageView btnFavorito;
    
    private FavoritesManager favoritesManager;
    private Character currentCharacter;
    private Call<List<Character>> characterRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character_perfil);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnCall).setOnClickListener(v -> {
            if (currentCharacter != null) startActivity(Camera.intent(this, currentCharacter));
        });

        favoritesManager = new FavoritesManager(this);
        iniciarViews();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        
        btnFavorito.setOnClickListener(v -> {
            if (currentCharacter != null) {
                favoritesManager.toggleFavorite(currentCharacter);
                updateFavoriteIcon();
                String msg = favoritesManager.isFavorite(currentCharacter.id) 
                    ? currentCharacter.name + " favoritado!" 
                    : currentCharacter.name + " removido!";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnCompartilhar).setOnClickListener(v -> {
            if (currentCharacter != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Confira o personagem " + currentCharacter.name + " no MarvelCerto!");
                startActivity(Intent.createChooser(shareIntent, "Compartilhar com..."));
            }
        });

        long characterId = getIntent().getLongExtra(EXTRA_CHARACTER_ID, -1);

        if (characterId == -1) {
            Toast.makeText(this, "Personagem inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        carregarPersonagem(characterId);
    }

    private void iniciarViews() {
        imgHero = findViewById(R.id.imgHero);
        txtNomePersonagem = findViewById(R.id.txtNomePersonagem);
        txtNomeReal = findViewById(R.id.txtNomeReal);
        txtBiografia = findViewById(R.id.txtBiografia);
        txtEditora = findViewById(R.id.txtEditora);
        txtAlinhamento = findViewById(R.id.txtAlinhamento);
        txtAparencia = findViewById(R.id.txtAparencia);
        txtOcupacao = findViewById(R.id.txtOcupacao);
        txtAfiliacao = findViewById(R.id.txtAfiliacao);
        txtEstatisticas = findViewById(R.id.txtEstatisticas);
        btnFavorito = findViewById(R.id.btnFavorito);
    }

    private void carregarPersonagem(long id) {
        characterRequest = MarvelApiService.getApi().getAll();
        characterRequest.enqueue(new Callback<List<Character>>() {
            @Override
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                if (isDestroyed() || isFinishing()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CharacterPerfilActivity.this, "Erro ao carregar", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Character p : response.body()) {
                    if (p.id == id) {
                        currentCharacter = p;
                        preencherTela(p);
                        preencherContexto(p, response.body());
                        findViewById(R.id.btnCall).setEnabled(true);
                        updateFavoriteIcon();
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Character>> call, Throwable t) {
                if (call.isCanceled() || isDestroyed()) return;
                Toast.makeText(CharacterPerfilActivity.this, "Falha na conexão", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteIcon() {
        if (currentCharacter != null) {
            boolean isFav = favoritesManager.isFavorite(currentCharacter.id);
            btnFavorito.setImageResource(isFav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            btnFavorito.setColorFilter(getColor(isFav ? R.color.marvel_gold : R.color.text_primary));
            btnFavorito.setContentDescription(isFav ? "Remover dos favoritos" : "Adicionar aos favoritos");
        }
    }

    private void preencherTela(Character personagem) {
        txtNomePersonagem.setText(personagem.name.toUpperCase());
        txtNomeReal.setText(personagem.biography != null ? textoOuTraco(personagem.biography.fullName) : "—");

        if (personagem.getImageUrl() != null) {
            Glide.with(this).load(personagem.getImageUrl()).into(imgHero);
        }

        txtBiografia.setText(personagem.getShortDescription());
        
        if (personagem.biography != null) {
            txtEditora.setText("Editora: " + textoOuTraco(personagem.biography.publisher));
            txtAlinhamento.setText("Alinhamento: " + textoOuTraco(personagem.biography.alignment));
        }

        if (personagem.appearance != null) {
            String raca = textoOuTraco(personagem.appearance.race);
            String genero = textoOuTraco(personagem.appearance.gender);
            txtAparencia.setText("Raça: " + raca + "\nGênero: " + genero + "\nOlhos: " + textoOuTraco(personagem.appearance.eyeColor));
        }

        if (personagem.work != null) {
            txtOcupacao.setText("Ocupação: " + textoOuTraco(personagem.work.occupation));
        }

        if (personagem.connections != null) {
            txtAfiliacao.setText("Afiliação: " + textoOuTraco(personagem.connections.groupAffiliation));
        }

        if (personagem.powerstats != null) {
            txtEstatisticas.setText(
                    "INTELIGÊNCIA: " + personagem.powerstats.intelligence +
                    "\nFORÇA: " + personagem.powerstats.strength +
                    "\nVELOCIDADE: " + personagem.powerstats.speed +
                    "\nDURABILIDADE: " + personagem.powerstats.durability +
                    "\nPODER: " + personagem.powerstats.power +
                    "\nCOMBATE: " + personagem.powerstats.combat
            );
        }
    }

    @Override protected void onDestroy() {
        if (characterRequest != null) characterRequest.cancel();
        super.onDestroy();
    }
    private String textoOuTraco(String texto) {
        return (texto == null || texto.isEmpty() || texto.equals("-")) ? "—" : texto;
    }

    private void preencherContexto(Character character, List<Character> all) {
        boolean villain = character.isVillain();
        boolean thanos = "Thanos".equalsIgnoreCase(character.name);
        ((TextView)findViewById(R.id.txtRole)).setText(thanos ? "VILÃO COSMOLÓGICO" : villain ? "VILÃO · MARVEL" : "HERÓI · MARVEL");
        ((TextView)findViewById(R.id.txtBioTitle)).setText(villain ? "▎ AMEAÇA" : "▎ BIOGRAFIA");
        ((TextView)findViewById(R.id.txtOpponentsTitle)).setText(villain ? "▎ HERÓIS QUE O ENFRENTARAM" : "▎ INIMIGOS DO MCU");
        String[] values;
        String[] labels;
        if (thanos) {
            txtNomeReal.setText("O Titã Louco");
            txtBiografia.setText("Titã exilado que busca equilibrar o universo eliminando metade de toda vida. Maior antagonista da Saga do Infinito.");
            values = new String[]{"4", "6", "50%", "∞"};
            labels = new String[]{"FILMES", "JOIAS", "SNAP", "PODER"};
        } else {
            values = character.powerstats == null ? new String[]{"—", "—", "—", "—"} : new String[]{
                String.valueOf(character.powerstats.intelligence), String.valueOf(character.powerstats.strength),
                String.valueOf(character.powerstats.speed), String.valueOf(character.powerstats.power)};
            labels = new String[]{"INTELECTO", "FORÇA", "VELOCIDADE", "PODER"};
        }
        int[] valueIds = {R.id.summaryValue1, R.id.summaryValue2, R.id.summaryValue3, R.id.summaryValue4};
        int[] labelIds = {R.id.summaryLabel1, R.id.summaryLabel2, R.id.summaryLabel3, R.id.summaryLabel4};
        for (int i = 0; i < 4; i++) {
            ((TextView)findViewById(valueIds[i])).setText(values[i]);
            ((TextView)findViewById(labelIds[i])).setText(labels[i]);
        }
        txtAlinhamento.setText("Alinhamento: " + (villain ? "Vilão" : character.biography != null && "good".equalsIgnoreCase(character.biography.alignment) ? "Herói" : "Neutro / desconhecido"));
        McuProfile profile = McuProfile.forName(character.name);
        LinearLayout films = findViewById(R.id.filmography);
        films.removeAllViews();
        ((TextView)findViewById(R.id.txtFilmographyInfo)).setText(profile.movies.isEmpty()
                ? "Filmografia ainda não cadastrada para este personagem."
                : profile.movies.size() + " filmes cadastrados · deslize para explorar");
        for (McuProfile.Movie movie : profile.movies) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(20), dp(16), dp(20));
            card.setBackgroundResource(R.drawable.bg_profile_card);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(160), LinearLayout.LayoutParams.MATCH_PARENT);
            size.setMarginEnd(dp(10));
            films.addView(card, size);
            TextView year = new TextView(this);
            year.setText(String.valueOf(movie.year));
            year.setTextSize(24);
            year.setTextColor(getColor(R.color.marvel_gold));
            card.addView(year);
            TextView title = new TextView(this);
            title.setText(movie.title);
            title.setTextSize(14);
            title.setTextColor(getColor(R.color.text_primary));
            title.setPadding(0, dp(16), 0, 0);
            card.addView(title);
        }
        LinearLayout container = findViewById(R.id.opponents);
        container.removeAllViews();
        if (profile.opponents.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(villain ? "Heróis que enfrentaram este vilão ainda não cadastrados."
                    : "Confrontos do MCU ainda não cadastrados para este personagem.");
            empty.setTextColor(getColor(R.color.text_secondary));
            empty.setPadding(0, dp(12), 0, dp(12));
            container.addView(empty);
        }
        for (McuProfile.Opponent opponent : profile.opponents) {
            Character target = null;
            for (Character other : all) {
                if (opponent.apiName.equalsIgnoreCase(other.name)) { target = other; break; }
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_profile_card);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            row.setMinimumHeight(dp(64));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2);
            size.topMargin = dp(8);
            container.addView(row, size);
            if (target != null && target.getImageUrl() != null) {
                ImageView portrait = new ImageView(this);
                portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);
                portrait.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams portraitSize = new LinearLayout.LayoutParams(dp(56), dp(64));
                portraitSize.setMarginEnd(dp(12));
                row.addView(portrait, portraitSize);
                Glide.with(this).load(target.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery).into(portrait);
            }
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            TextView name = new TextView(this);
            name.setText(opponent.name + (target == null ? "" : "  →"));
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(15);
            details.addView(name);
            TextView movies = new TextView(this);
            movies.setText(opponent.movies);
            movies.setTextColor(getColor(R.color.text_secondary));
            movies.setPadding(0, dp(4), 0, 0);
            movies.setTextSize(12);
            details.addView(movies);
            if (target != null) {
                final long targetId = target.id;
                row.setFocusable(true);
                row.setContentDescription("Abrir perfil de " + opponent.name + ". " + opponent.movies);
                row.setOnClickListener(v -> startActivity(new Intent(this, CharacterPerfilActivity.class)
                        .putExtra(EXTRA_CHARACTER_ID, targetId)));
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
