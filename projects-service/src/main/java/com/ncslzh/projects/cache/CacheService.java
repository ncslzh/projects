package com.ncslzh.projects.cache;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Cache Service
 *
 */
public interface CacheService {

    /**
     * Get a string value by the key
     *
     * @param key The cache key.
     * @return The cache value.
     */
    String get(String key);

    /**
     * Get a string value by the key, and casts it to a class specified
     *
     * @param key The cache key.
     * @param targetCastClass class that the String value will be casted to
     * @return The cache value.
     */
    <T> T getCastedString(String key, Class<T> targetCastClass);

    <T> T get(String key, int expireSeconds, Class<T> clazz, Supplier<T> supplier);


    Long increment(String key);

    /**
     * Set without timeout.
     *
     * @param key   The cache key.
     * @param value The String value.
     */
    void set(String key, String value);

    /**
     * Set with timeout
     *
     * @param key      The cache key.
     * @param value    The String value.
     * @param duration Timeout duration.
     */
    void set(String key, String value, Duration duration);

    Boolean setIfAbsent(String key, String value, Duration duration);

    /**
     * Delete the cache key.
     *
     * @param key The cache key.
     */
    void delete(String key);

    void delete(List<String> keys);

    Boolean expire(String key, Duration duration);

    Long getExpire(String key);

    Long getExpire(String key, TimeUnit timeUnit);

    Boolean hasKey(String key);

    List<String> multiGet(Collection<String> keys);

    String hashGet(String key, String hashKey);

    void hashDelete(String key, String hashKey);

    void hashPut(String key, String hashKey, String value);

    List<String> hashMultiGet(String key, Collection<String> hashKeys);

    void hashPutAll(String key, Map<String, String> hashKeys);

    Long hashGetSize(String key);

    List<Object> values(String key);

    Map<String, String> entries(String key);

    Set<Object> keys(String key);

    Double zScore(String key, String member);

    Set<String> zRangeByScore(String key, Long start, Long end);

    Set<ZSetOperations.TypedTuple<String>> zRangeByScoreWithScores(
            String key, Double start, Double end);

    Set<String> getRangeByScoreFromZSet(String key, Double start, Double end);

    Set<String> zRevRange(String key, Long start, Long end);

    Set<ZSetOperations.TypedTuple<String>> reverseRangeWithScores(String key, Long start, Long end);

    Long zCard(String key);

    void removeRangeByScore(String key, Double start, Double end);

    void add(String key, String value, Long score);

    void addToZSet(String key, String value, Double score);

    void add(String key, Set<ZSetOperations.TypedTuple<String>> value);

    void removeRange(String key, Long start, Long end);

    void removeFromZSet(String key, String value);

    void remove(String key, String values);

    Long count(String key, Double min, Double max);

    <T> T execute(RedisCallback<T> action);

    <T> T executeGet(RedisCallback<T> action);

    <T> List<Object> executePipelined(RedisCallback<T> action);

    List<String> range(String key, Long start, Long end);

    Set<ZSetOperations.TypedTuple<String>> zIntersectWithScores(String key, String otherKey);

    Set<String> zSetRange(String key, Long start, Long end);

    void leftPush(String key, String value);

    void leftPushAll(String key, List<String> lst);

    String boundValueOpsGet(String key);

    void boundValueOpsSet(String key, String value, Duration duration);

    void convertAndSend(String channel, Object message);

    void renameKey(String oldKey, String newKey);

    <T> List<T> list(String key, int expireSeconds, Class<T> clazz, Supplier<List<T>> supplier);

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

    // Set operation in redis
    void addSet(String key, String... values);
    Set<String> getSet(String key);
    Long removeSet(String key, Object... values);
    Set<String> intersectSet(String baseKey, String key);

}
