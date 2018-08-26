package com.creations.livebox.datasources;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface LocalDataSource<I, T> {
    T read();

    void save(I input);
}
