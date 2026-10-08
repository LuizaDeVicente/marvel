package com.example.marvelcerto;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvelcerto.adapter.CharacterAdapter;
import com.example.marvelcerto.api.Character;
import com.example.marvelcerto.api.MarvelApiService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText edtSearch;
    private RecyclerView rvResults;
    private CharacterAdapter adapter;
    private BottomNavigationView bottomNav;
    private final List<Character> characters = new ArrayList<>();
    private Call<List<Character>> request;
    private boolean loading;
    private boolean loaded;
    private boolean failed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtSearch = findViewById(R.id.edtSearch);
        rvResults = findViewById(R.id.rvResults);

        adapter = new CharacterAdapter(this);
        ResponsiveGrid.configure(rvResults, 168);
        adapter.setOnCharacterClickListener(character -> {
            Intent intent = new Intent(SearchActivity.this, CharacterPerfilActivity.class);
            intent.putExtra(CharacterPerfilActivity.EXTRA_CHARACTER_ID, character.id);
            startActivity(intent);
        });
        rvResults.setAdapter(adapter);

        setupBottomNavigation();
        findViewById(R.id.fabQuiz).setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateResults();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        String query = savedInstanceState == null ? getIntent().getStringExtra("search_query")
                : savedInstanceState.getString("search_query");
        if (query != null) {
            edtSearch.setText(query);
            edtSearch.setSelection(edtSearch.length());
        }
        loadCharacters();
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_search);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, PlayerProfileActivity.class));
                return false;
            } else if (itemId == R.id.nav_camera) {
                startActivity(new Intent(this, CharacterPickerActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void updateResults() {
        String query = edtSearch.getText().toString().trim();
        List<Character> results = CharacterSearch.find(characters, query);
        adapter.setCharacters(results);
        String message = loading && !loaded ? "Carregando personagens…"
                : failed ? (loaded ? "Sem conexão. Exibindo personagens salvos." : "Não foi possível carregar a busca. Verifique sua conexão.")
                : query.isEmpty() ? "Digite o nome de um herói ou vilão para buscar."
                : loaded && results.isEmpty() ? "Nenhum personagem encontrado. Tente outro nome." : null;
        ListState.show(this, message, loading, failed ? this::loadCharacters : null);
    }

    private void loadCharacters() {
        if (loading) return;
        List<Character> cached = new GameStore(this).catalog();
        if (!cached.isEmpty() && !loaded) {
            characters.clear();
            characters.addAll(cached);
            loaded = true;
        }
        loading = true;
        failed = false;
        updateResults();
        request = MarvelApiService.getApi().getAll();
        request.enqueue(new Callback<List<Character>>() {
            @Override public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                if (isDestroyed() || isFinishing()) return;
                loading = false;
                failed = !response.isSuccessful() || response.body() == null;
                if (!failed) {
                    characters.clear();
                    characters.addAll(response.body());
                    new GameStore(SearchActivity.this).cache(characters);
                    loaded = true;
                }
                // Always use the current text, even if it changed while the request was in flight.
                updateResults();
            }
            @Override public void onFailure(Call<List<Character>> call, Throwable t) {
                if (call.isCanceled() || isDestroyed() || isFinishing()) return;
                loading = false;
                failed = true;
                updateResults();
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("search_query", edtSearch.getText().toString());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }
}
