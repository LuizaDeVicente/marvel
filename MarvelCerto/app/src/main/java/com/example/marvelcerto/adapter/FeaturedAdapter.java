package com.example.marvelcerto.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marvelcerto.R;
import com.example.marvelcerto.api.Character;

import java.util.ArrayList;
import java.util.List;

public class FeaturedAdapter extends RecyclerView.Adapter<FeaturedAdapter.FeaturedViewHolder> {

    public interface OnCharacterClickListener {
        void onCharacterClick(Character character);
    }

    private List<Character> characters = new ArrayList<>();
    private OnCharacterClickListener listener;

    public void setOnCharacterClickListener(OnCharacterClickListener listener) {
        this.listener = listener;
    }

    public void setCharacters(List<Character> characters) {
        this.characters = characters != null ? characters : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        Character character = characters.get(position);
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

    static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgFeatured;
        private TextView txtFeaturedTitle;
        private TextView txtFeaturedSubtitle;

        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFeatured = itemView.findViewById(R.id.imgFeatured);
            txtFeaturedTitle = itemView.findViewById(R.id.txtFeaturedTitle);
            txtFeaturedSubtitle = itemView.findViewById(R.id.txtFeaturedSubtitle);
        }

        public void bind(Character character) {
            txtFeaturedTitle.setText(character.name != null ? character.name : "");
            
            String publisher = (character.biography != null && character.biography.publisher != null)
                    ? character.biography.publisher : "";
            String alignment = (character.biography != null && character.biography.alignment != null)
                    ? character.biography.alignment : "";
            String stats = "";
            if (character.powerstats != null) {
                stats = "STR:" + character.powerstats.strength + " • POW:" + character.powerstats.power;
            }
            String subtitle = publisher;
            if (!alignment.isEmpty()) {
                subtitle += (subtitle.isEmpty() ? "" : " • ") + alignment;
            }
            if (!stats.isEmpty()) {
                subtitle += (subtitle.isEmpty() ? "" : " • ") + stats;
            }
            txtFeaturedSubtitle.setText(subtitle);

            String imageUrl = character.getImageUrl();
            if (imageUrl != null) {
                Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(imgFeatured);
            } else {
                imgFeatured.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }
}
