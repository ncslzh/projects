package com.ncslzh.projects.cache;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Allow Redis keys to have a refresh duration where keys are not expired but asynchronously re-loaded in order
 * to prioritise availability over consistency.
 * <br>See Caffeine cache refreshAfter configuration
 */
public interface RefreshableCacheService extends CacheService {
    /**
     * If key does not exist in cache, stores a new {@code RefreshableKey } and returns the value from {@code supplier}
     * Otherwise, returns the key whilst refreshing value asynchronously from {@code supplier}
     * i.e. Old value is returned
     */
    <T> String getRefreshableFallback(String key, Duration refreshAfter, Duration expireAfter, Supplier<T> supplier);

    /**
     * Same as above, but casts into desired {@code clazz}
     */
    <T> T getRefreshableFallback(String key, Class<T> clazz, Duration refreshAfter, Duration expireAfter, Supplier<T> supplier);

    /**
     * Sets a {@code RefreshableKey} that attempts to refresh the first key access after {@code refreshAfter} and does not expire
     */
    void setRefreshable(String key, String value, Duration refreshAfter);

    /**
     * Sets a {@code RefreshableKey} that attempts to refresh the first key access after {@code refreshAfter} and expires
     * after {@code expreAfter}
     *
     * <p> If {@code expireAfter} < {@code refreshAfter} the key will always expire before triggering refresh, in which case why are you using this
     */
    void setRefreshable(String key, String value, Duration refreshAfter, Duration expireAfter);
}
