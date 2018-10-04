package com.creations.livebox.datasources.factory;

import com.creations.livebox.datasources.LocalDataSource;
import com.creations.livebox.util.Optional;

/**
 * @author Sérgio Serra on 26/08/2018.
 * Criations
 * sergioserra99@gmail.com
 */
public interface DataSourceFactory<I> {
    Optional<LocalDataSource<I>> get(int id);
}
