package com.creations.app.api;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * @author Sérgio Serra
 * sergioserra99@gmail.com
 */
public class Api {

    private static final Api ourInstance = new Api();
    private GithubService githubService;

    private Api() {
        githubService = build();
    }

    public static Api getInstance() {
        return ourInstance;
    }

    private static <T> T builder(Class<T> endpoint) {
        return new Retrofit.Builder()
                .baseUrl(Config.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
                .create(endpoint);
    }

    private static GithubService build() {
        return builder(GithubService.class);
    }

    public GithubService getGithubService() {
        return githubService;
    }

    public static class Config {
        public static final String BASE_URL = "https://api.github.com";
    }

}
