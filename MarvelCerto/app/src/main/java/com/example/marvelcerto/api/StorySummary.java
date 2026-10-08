package com.example.marvelcerto.api;

import com.google.gson.annotations.SerializedName;

public class StorySummary {
    @SerializedName("resourceURI")
    public String resourceURI;

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;
}
