package com.example.marvelcerto;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marvelcerto.adapter.CharacterAdapter;
import com.example.marvelcerto.api.Character;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private CharacterAdapter adapter;
    private List<Character> favoriteCharacters = new ArrayList<>();
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvFavorites = findViewById(R.id.rvFavorites);
        adapter = new CharacterAdapter(this);
        ResponsiveGrid.configure(rvFavorites, 168);
        adapter.setOnFavoritesChangedListener(this::loadFavorites);
        adapter.setOnCharacterClickListener(character -> {
            Intent intent = new Intent(FavoritesActivity.this, CharacterPerfilActivity.class);
            intent.putExtra(CharacterPerfilActivity.EXTRA_CHARACTER_ID, character.id);
            startActivity(intent);
        });
        rvFavorites.setAdapter(adapter);

        setupBottomNavigation();
        findViewById(R.id.fabQuiz).setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
    }

    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_favorites);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_favorites) {
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

    private void loadFavorites() {
        FavoritesManager favoritesManager = new FavoritesManager(this);
        favoriteCharacters = favoritesManager.getAllFavorites();
        adapter.setCharacters(favoriteCharacters);
        ListState.show(this, favoriteCharacters.isEmpty()
                ? "Você ainda não tem favoritos. Toque na estrela de um personagem para salvá-lo aqui." : null,
                false, null);
    }

    @Override protected void onResume() {
        super.onResume();
        if (adapter != null) loadFavorites();
    }
}
