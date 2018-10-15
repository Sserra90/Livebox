package com.creations.app;

import com.creations.app.api.User;
import com.creations.convert_jackson.util.Util;
import com.creations.livebox.LiveboxBuilder;
import com.creations.livebox.converters.Converter;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory;
import com.creations.livebox.datasources.factory.LiveboxDataSourceFactory.Sources;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;

import static com.creations.convert_jackson.util.Util.fromRef;

class UsersRepo {

    Observable<List<Integer>> getUsers() {

        final Type type = fromRef(new Util.TypeRef<List<String>>() {
        });

        final List<String> data = new ArrayList<>();
        data.add("1");

        return new LiveboxBuilder<List<String>, List<Integer>>()
                .withKey("some_key")
                .fetch(() -> Observable.just(data), type)
                .addSource(Sources.DISK_LRU, (a, b) -> true)
                .addConverter(type, (Converter<List<String>, List<Integer>>) t -> new ArrayList<>())
                .build()
                .asAndroidObservable();
    }
}
