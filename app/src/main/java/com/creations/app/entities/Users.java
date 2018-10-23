package com.creations.app.entities;

import com.creations.app.api.User;
import com.creations.app.api.UsersRes;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.annotations.NonNull;

/**
 * @author Sérgio Serra on 26/08/2018.
 * sergioserra99@gmail.com
 * <p>
 * <p>
 * Dummy representation of Users in domain layer.
 */
public class Users {

    private List<String> items;

    Users(List<String> items) {
        this.items = items;
    }

    @NonNull
    public static Users fromUsersRes(UsersRes usersRes) {
        List<String> usersList = new ArrayList<>();
        for (User user : usersRes.getItems()) {
            usersList.add(user.getHtmlUrl());
        }
        return new Users(usersList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return items.equals(users.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return "Users{" +
                "items=" + items +
                '}';
    }
}
