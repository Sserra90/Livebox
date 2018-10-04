package com.creations.livebox.datasources;

import com.creations.livebox.util.Optional;

/**
 * @author Sérgio Serra on 25/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface LocalDataSource<I> {
    Optional<I> read(String key);

    void save(String key, I input);

    void clear(String key);
}
