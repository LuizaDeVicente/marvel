package com.example.marvelcerto.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Character {
    public boolean isVillain() {
        return biography != null && "bad".equalsIgnoreCase(biography.alignment);
    }
    @SerializedName("id")
    public long id;

    @SerializedName("name")
    public String name;

    @SerializedName("slug")
    public String slug;

    @SerializedName("powerstats")
    public Powerstats powerstats;

    @SerializedName("appearance")
    public Appearance appearance;

    @SerializedName("biography")
    public Biography biography;

    @SerializedName("work")
    public Work work;

    @SerializedName("connections")
    public Connections connections;

    @SerializedName("images")
    public Images images;

    public String getImageUrl() {
        if (images != null) {
            if (images.md != null && !images.md.isEmpty()) return images.md;
            if (images.sm != null && !images.sm.isEmpty()) return images.sm;
            if (images.lg != null && !images.lg.isEmpty()) return images.lg;
            if (images.xs != null && !images.xs.isEmpty()) return images.xs;
        }
        return null;
    }

    public String getShortDescription() {
        if (biography != null && biography.fullName != null && !biography.fullName.isEmpty()) {
            return biography.fullName;
        }
        if (work != null && work.occupation != null && !work.occupation.isEmpty()) {
            return work.occupation;
        }
        return "Sem descrição";
    }

    public static class Powerstats {
        public int intelligence;
        public int strength;
        public int speed;
        public int durability;
        public int power;
        public int combat;
    }

    public static class Appearance {
        public String gender;
        public String race;
        public List<String> height;
        public List<String> weight;
        public String eyeColor;
        public String hairColor;
    }

    public static class Biography {
        @SerializedName("fullName")
        public String fullName;

        @SerializedName("alterEgos")
        public String alterEgos;

        public List<String> aliases;

        @SerializedName("placeOfBirth")
        public String placeOfBirth;

        @SerializedName("firstAppearance")
        public String firstAppearance;

        public String publisher;
        public String alignment;
    }

    public static class Work {
        public String occupation;
        public String base;
    }

    public static class Connections {
        public String groupAffiliation;
        public String relatives;
    }

    public static class Images {
        public String xs;
        public String sm;
        public String md;
        public String lg;
    }
}
