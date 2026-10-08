package com.example.marvelcerto.api;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

import java.util.List;

public class MarvelApiService {
    private static final String BASE_URL = "https://akabab.github.io/superhero-api/api/";

    public interface MarvelApi {
        @GET("all.json")
        Call<List<Character>> getAll();
    }

    private static MarvelApi api;
    private static Retrofit retrofit;

    public static MarvelApi getApi() {
        if (api == null) {
            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

            api = retrofit.create(MarvelApi.class);
        }
        return api;
    }
}
