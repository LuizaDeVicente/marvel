package com.example.marvelcerto.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.Configuration;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marvelcerto.FavoritesManager;
import com.example.marvelcerto.R;
import com.example.marvelcerto.api.Character;

import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    public interface OnCharacterClickListener {
        void onCharacterClick(Character character);
    }

    private List<Character> characters = new ArrayList<>();
    private FavoritesManager favoritesManager;
    private Context context;
    private OnCharacterClickListener listener;
    private Runnable onFavoritesChanged;

    public void setOnFavoritesChangedListener(Runnable listener) {
        onFavoritesChanged = listener;
    }

    public CharacterAdapter() {
        this.context = null;
        this.favoritesManager = null;
    }

    public CharacterAdapter(Context context) {
        this.context = context;
        this.favoritesManager = new FavoritesManager(context);
    }

    public void setOnCharacterClickListener(OnCharacterClickListener listener) {
        this.listener = listener;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters != null ? characters : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_character_card, parent, false);
        if (context == null) {
            context = parent.getContext();
            favoritesManager = new FavoritesManager(context);
        }
        return new CharacterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Character character = characters.get(position);
        holder.onFavoritesChanged = onFavoritesChanged;
        holder.bind(character);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCharacterClick(character);
            }
        });
    }

    @Override
    public int getItemCount() {
        return characters.size();
    }

    static class CharacterViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgCharacter;
        private TextView txtName;
        private TextView txtRealName;
        private TextView txtMovies;
        private ImageView btnFavorite;
        private Character boundCharacter;
        private Runnable onFavoritesChanged;
        private FavoritesManager favoritesManager;

        public CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCharacter = itemView.findViewById(R.id.imgCharacter);
            txtName = itemView.findViewById(R.id.txtName);
            txtRealName = itemView.findViewById(R.id.txtRealName);
            txtMovies = itemView.findViewById(R.id.txtMovies);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(Character character) {
            this.boundCharacter = character;
            this.favoritesManager = new FavoritesManager(itemView.getContext());
            ViewGroup.LayoutParams cardParams = itemView.getLayoutParams();
            if (cardParams != null) {
                float fontScale = itemView.getResources().getConfiguration().fontScale;
                cardParams.height = Math.round(220 * itemView.getResources().getDisplayMetrics().density * Math.max(1f, fontScale));
                itemView.setLayoutParams(cardParams);
            }
            String alignmentDescription = character.biography != null && character.biography.alignment != null
                    ? character.biography.alignment : "alinhamento desconhecido";
            itemView.setContentDescription(character.name + ", " + alignmentDescription + ". Toque para ver detalhes.");
            imgCharacter.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            txtName.setText(character.name != null ? character.name : "");
            String shortDesc = character.getShortDescription();
            txtRealName.setText(shortDesc != null && shortDesc.length() > 30
                ? shortDesc.substring(0, 30) + "..."
                : shortDesc != null ? shortDesc : "");

            // Publisher / editoras
            String publisher = (character.biography != null && character.biography.publisher != null)
                    ? character.biography.publisher : "Desconhecido";
            txtMovies.setText(publisher);

            // Alignment
            TextView txtAlignment = itemView.findViewById(R.id.txtAlignment);
            String alignment = (character.biography != null && character.biography.alignment != null)
                    ? character.biography.alignment : "Desconhecido";
            txtAlignment.setText(alignment);

            // Powerstats summary
            TextView txtStats = itemView.findViewById(R.id.txtStats);
            if (character.powerstats != null) {
                String stats = "STR: " + character.powerstats.strength + " • POW: " + character.powerstats.power;
                txtStats.setText(stats);
            } else {
                txtStats.setText("STR: - • POW: -");
            }

            String imageUrl = character.getImageUrl();
            if (imageUrl != null) {
                Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgCharacter);
            } else {
                imgCharacter.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Configurar estrela de favorito
            updateFavoriteIcon();

            btnFavorite.setOnClickListener(v -> {
                if (boundCharacter != null && favoritesManager != null) {
                    favoritesManager.toggleFavorite(boundCharacter);
                    updateFavoriteIcon();

                    String message = favoritesManager.isFavorite(boundCharacter.id)
                        ? boundCharacter.name + " adicionado aos favoritos"
                        : boundCharacter.name + " removido dos favoritos";
                    Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                    if (onFavoritesChanged != null) onFavoritesChanged.run();
                }
            });
        }

        private void updateFavoriteIcon() {
            if (boundCharacter != null && favoritesManager != null) {
                boolean isFavorite = favoritesManager.isFavorite(boundCharacter.id);
                btnFavorite.setContentDescription((isFavorite ? "Remover dos favoritos: " : "Favoritar: ") + boundCharacter.name);
                if (isFavorite) {
                    btnFavorite.setImageResource(R.drawable.ic_star_filled);
                    btnFavorite.setColorFilter(0xFFF0B323);
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_star_outline);
                    btnFavorite.setColorFilter(0xFFFFFFFF);
                }
            }
        }
    }
}
