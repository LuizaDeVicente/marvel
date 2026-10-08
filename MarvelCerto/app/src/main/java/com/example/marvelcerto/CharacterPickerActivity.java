package com.example.marvelcerto;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.marvelcerto.adapter.CharacterAdapter;
import com.example.marvelcerto.api.Character;
import com.example.marvelcerto.api.MarvelApiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CharacterPickerActivity extends AppCompatActivity {
    public static final String EXTRA_AVATAR = "choose_avatar";
    private boolean choosingAvatar;
    private final List<Character> characters = new ArrayList<>();
    private CharacterAdapter adapter;
    private EditText search;
    private RadioGroup filters;
    private TextView status;
    private Call<List<Character>> request;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_character_picker);
        choosingAvatar = getIntent().getBooleanExtra(EXTRA_AVATAR, false);
        if (choosingAvatar) {
            ((TextView)findViewById(R.id.pickerTitle)).setText("SUA FOTO DE PERFIL");
            ((TextView)findViewById(R.id.pickerSubtitle)).setText("Escolha qualquer personagem. Não precisa desbloqueá-lo para usar como avatar.");
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        search = findViewById(R.id.edtSearch);
        filters = findViewById(R.id.filters);
        status = findViewById(R.id.txtStatus);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        adapter = new CharacterAdapter(this);
        adapter.setOnCharacterClickListener(c -> {
            if (choosingAvatar) { new GameStore(this).avatar(c); finish(); }
            else startActivity(Camera.intent(this, c));
        });
        RecyclerView list = findViewById(R.id.rvCharacters);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        ResponsiveGrid.configure(list, 168);
        list.setAdapter(adapter);
        filters.setOnCheckedChangeListener((group, id) -> filter());
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.btnRetry).setOnClickListener(v -> load());
        load();
    }
    private void load() {
        List<Character> cached = new GameStore(this).catalog();
        if (!cached.isEmpty()) populate(cached);
        status.setText(cached.isEmpty() ? "Carregando personagens…" : "Exibindo personagens salvos · atualizando…");
        findViewById(R.id.btnRetry).setVisibility(View.GONE);
        request = MarvelApiService.getApi().getAll();
        request.enqueue(new Callback<List<Character>>() {
            public void onResponse(Call<List<Character>> call, Response<List<Character>> response) {
                if (isDestroyed()) return;
                if (!response.isSuccessful() || response.body() == null) { error(!cached.isEmpty()); return; }
                new GameStore(CharacterPickerActivity.this).cache(response.body());
                populate(response.body());
            }
            public void onFailure(Call<List<Character>> call, Throwable t) { if (!call.isCanceled() && !isDestroyed()) error(!cached.isEmpty()); }
        });
    }
    private void error(boolean hasCache) {
        status.setText(hasCache ? "Sem conexão. Exibindo personagens salvos." : "Não foi possível carregar. Verifique sua conexão.");
        findViewById(R.id.btnRetry).setVisibility(View.VISIBLE);
    }
    private void populate(List<Character> source) {
        characters.clear();
        for (Character c : source) {
            if (c.biography != null && c.biography.publisher != null && c.biography.publisher.toLowerCase(Locale.ROOT).contains("marvel")) characters.add(c);
        }
        filter();
    }
    private void filter() {
        List<Character> result = new ArrayList<>();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        for (Character c : characters) {
            boolean match = filters.getCheckedRadioButtonId() == R.id.filterAll
                    || (filters.getCheckedRadioButtonId() == R.id.filterVillains && c.isVillain())
                    || (filters.getCheckedRadioButtonId() == R.id.filterHeroes && c.biography != null && "good".equalsIgnoreCase(c.biography.alignment));
            if (match && c.name != null && c.name.toLowerCase(Locale.ROOT).contains(q)) result.add(c);
        }
        adapter.setCharacters(result);
        status.setText(result.isEmpty() ? "Nenhum personagem encontrado." : result.size() + (choosingAvatar ? " personagens · toque para escolher" : " personagens · toque para chamar"));
    }
    @Override protected void onDestroy() { if (request != null) request.cancel(); super.onDestroy(); }
}
