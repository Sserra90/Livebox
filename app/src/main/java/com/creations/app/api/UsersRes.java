package com.creations.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 *
 * Representation of GitHub service API response from getUsers endpoint.
 */
public class UsersRes {

    @SerializedName("items")
    private List<User> items = new ArrayList<>();

    public List<User> getItems() {
        return items;
    }

    public void setItems(List<User> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "UsersRes{" +
                "items=" + items +
                '}';
    }
}
