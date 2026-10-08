package com.example.marvelcerto.api;

import com.google.gson.annotations.SerializedName;

public class SeriesList {
    @SerializedName("available")
    public int available;

    @SerializedName("returned")
    public int returned;

    @SerializedName("collectionURI")
    public String collectionURI;

    @SerializedName("items")
    public SeriesSummary[] items;
}
