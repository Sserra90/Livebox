package com.creations.app.api;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Github service definition.
 */
public interface GithubService {

    @GET("/search/users?q=tom")
    Observable<Users> getUserList();
}
