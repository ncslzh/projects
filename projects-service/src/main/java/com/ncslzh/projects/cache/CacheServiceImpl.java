package com.ncslzh.projects.cache;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Cache Service implement.
 *
 * @author huston.peng
 */
@Slf4j
@Component
public class CacheServiceImpl implements CacheService {

//    @Qualifier("otcStringRedisTemplate")
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private DistributedLock distributedLock;
    @Resource(name = AsyncConfig.C2C_ASYNC_EXECUTOR)
    private Executor c2cAsyncExecutor;

    private static final String REFRESH_LOCK_S = "otc:refreshKey_%s";

    @Override
    public String get(String key) {
        return getValueOps().get(key);
    }

    @Override
    public <T> T getCastedString(String key, Class<T> clazz) {
        try {
            String value = get(key);
            if (StringUtils.isNotBlank(value)) {
                return JSON.parseObject(value, clazz);
            }
        } catch (Exception e) {
            log.warn("get0 error", e);
        }
        return null;
    }

    @Override
    public <T> T get(String key, int expireSeconds, Class<T> clazz, Supplier<T> supplier) {
        T obj = getCastedString(key, clazz);
        if (obj != null) {
            return obj;
        }

        obj = supplier.get();
        if (obj == null) {
            return null;
        }

        set(key, JSON.toJSONString(obj), Duration.ofSeconds(expireSeconds));
        return obj;
    }

    @Override
    public Long increment(String key) {
        return getValueOps().increment(key);
    }

    @Override
    public void set(String key, String value) {
        getValueOps().set(key, value);
    }

    @Override
    public void set(String key, String value, Duration duration) {
        getValueOps().set(key, value, duration);
    }

    /**
     * For distributed lock
     */
    @Override
    public Boolean setIfAbsent(String key, String value, Duration duration) {
        return getValueOps().setIfAbsent(key, value, duration);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }
    @Override
    public void delete(List<String> keys) {
        stringRedisTemplate.delete(keys);
    }
    @Override
    public Boolean expire(String key, Duration duration) {
        return stringRedisTemplate.expire(key, duration);
    }
    @Override
    public Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return stringRedisTemplate.getExpire(key, timeUnit);
    }

    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }
    @Override
    public List<String> multiGet(Collection<String> keys) {
        return getValueOps().multiGet(keys);
    }

    @Override
    public String hashGet(String key, String hashKey) {
        return getHashOps().get(key, hashKey);
    }

    @Override
    public void hashDelete(String key, String hashKey) {
        getHashOps().delete(key, hashKey);
    }

    @Override
    public void hashPut(String key, String hashKey, String value) {
        getHashOps().put(key, hashKey, value);
    }

    @Override
    public List<String> hashMultiGet(String key, Collection<String> hashKeys) {
        return getHashOps().multiGet(key, hashKeys);
    }

    @Override
    public void hashPutAll(String key, Map<String,String> hashKeys) {
        getHashOps().putAll(key, hashKeys);
    }

    @Override
    public Long hashGetSize(String key) {
        return getHashOps().size(key);
    }

    @Override
    public List<Object> values(String key) {
        return getObjectHashOps().values(key);
    }

    @Override
    public Map<String, String> entries(String key) {
        return getHashOps().entries(key);
    }

    @Override
    public Set<Object> keys(String key) {
        return getObjectHashOps().keys(key);
    }

    @Override
    public Double zScore(String key, String member) {
        return getZSetOps().score(key, member);
    }

    /**
     * @param start - start index (inclusive)
     * @param end - end index (inclusive)
     */
    @Override
    public Set<String> zSetRange(String key, Long start, Long end) {
        return getZSetOps().range(key, start, end);
    }

    @Override
    public Set<String> zRangeByScore(String key, Long start, Long end) {
        return getRangeByScoreFromZSet(key, start.doubleValue(), end.doubleValue());
    }

    @Override
    public Set<String> getRangeByScoreFromZSet(String key, Double start, Double end) {
        return getZSetOps().rangeByScore(key, start, end);
    }

    @Override
    public Set<TypedTuple<String>> zRangeByScoreWithScores(String key, Double start, Double end) {
        return getZSetOps().rangeByScoreWithScores(key, start, end);
    }

    @Override
    public Set<String> zRevRange(String key, Long start, Long end) {
        return getZSetOps().reverseRange(key, start, end);
    }

    @Override
    public Set<ZSetOperations.TypedTuple<String>> reverseRangeWithScores(
            String key, Long start, Long end) {
        return getZSetOps().reverseRangeWithScores(key, start, end);
    }

    @Override
    public void removeRange(String key, Long start, Long end) {
        getZSetOps().removeRange(key, start, end);
    }

    @Override
    public void removeRangeByScore(String key, Double start, Double end) {
        getZSetOps().removeRangeByScore(key, start, end);
    }

    @Override
    public void add(String key, String value, Long score) {
        getZSetOps().add(key, value, score);
    }

    @Override
    public void addToZSet(String key, String value, Double score) {
        getZSetOps().add(key, value, score);
    }

    @Override
    public void removeFromZSet(String key, String value) {
        getZSetOps().remove(key, value);
    }

    @Override
    public void remove(String key, String values) {
        getZSetOps().remove(key, values);
    }

    @Override
    public Long count(String key, Double min, Double max ) {
        return getZSetOps().count(key, min, max);
    }

    @Override
    public Long zCard(String key) {
        return getZSetOps().zCard(key);
    }

    @Override
    public Set<TypedTuple<String>> zIntersectWithScores(String key, String otherKey) {
        return getZSetOps().intersectWithScores(key, otherKey);
    }

    @Override
    public void add(String key, Set<ZSetOperations.TypedTuple<String>> value) {
        getZSetOps().add(key, value);
    }

    @Override
    public void leftPush(String key, String value) {
        getListOps().leftPush(key, value);
    }

    @Override
    public List<String> range(String key, Long start, Long end) {
        return getListOps().range(key, start, end);
    }

    @Override
    public void leftPushAll(String key,  List<String> lst) {
        getListOps().leftPushAll(key, lst);
    }

    @Override
    public void addSet(String key, String... values){
        getSetOps().add(key, values);
    }

    @Override
    public Set<String> intersectSet(String baseKey, String key){
        if (DataType.SET.equals(stringRedisTemplate.type(key)) && DataType.SET.equals(stringRedisTemplate.type(baseKey))){
            return getSetOps().intersect(baseKey, key);
        }
        return null;
    }

    @Override
    public Set<String> getSet(String key){
        return getSetOps().members(key);
    }

    @Override
    public Long removeSet(String key, Object... values){
        return getSetOps().remove(key, values);
    }

    @Override
    public <T> T execute(RedisCallback<T> action) {
        return stringRedisTemplate.execute(action);
    }

    @Override
    public <T> T executeGet(RedisCallback<T> action) {
        return stringRedisTemplate.execute(action);
    }

    @Override
    public <T> List<Object> executePipelined(RedisCallback<T> action) {
        return stringRedisTemplate.executePipelined(action);
    }

    @Override
    public String boundValueOpsGet(String key) {
        return getBoundValueOps(key).get();
    }

    @Override
    public void boundValueOpsSet(String key, String value, Duration duration) {
        getBoundValueOps(key).set(value, duration);
    }

    @Override
    public void convertAndSend(String channel, Object message) {
        stringRedisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void renameKey(String oldKey, String newKey) {
        stringRedisTemplate.rename(oldKey, newKey);
    }

    private <T> List<T> list0(String key, Class<T> clazz) {
        try {
            String value = get(key);
            if (StringUtils.isNotBlank(value)) {
                return JSON.parseArray(value, clazz);
            }
        } catch (Exception e) {
            log.warn("get0 error", e);
        }
        return new ArrayList<>();
    }

    @Override
    public <T> List<T> list(
            String key, int expireSeconds, Class<T> clazz, Supplier<List<T>> supplier) {

        List<T> objects = list0(key, clazz);
        if (objects != null && !objects.isEmpty()) {
            return objects;
        }

        objects = supplier.get();
        if (objects == null) {
            return new ArrayList<>();
        }

        set(key, JSON.toJSONString(objects), Duration.ofSeconds(expireSeconds));
        return objects;
    }
    private ZSetOperations<String, String> getZSetOps() {
        return stringRedisTemplate.opsForZSet();
    }

    private HashOperations<String, String, String> getHashOps() {
        return stringRedisTemplate.opsForHash();
    }

    private HashOperations<String, Object, Object> getObjectHashOps() {
        return stringRedisTemplate.opsForHash();
    }

    private ValueOperations<String, String> getValueOps() {
        return stringRedisTemplate.opsForValue();
    }

    private ListOperations<String, String> getListOps() {
        return stringRedisTemplate.opsForList();
    }

    private BoundValueOperations<String, String> getBoundValueOps(String key) {
        return stringRedisTemplate.boundValueOps(key);
    }

    private SetOperations<String, String> getSetOps() {
        return stringRedisTemplate.opsForSet();
    }

    @Override
    public <T> String getRefreshableFallback(String key, Duration refreshAfter, Duration expireAfter, Supplier<T> supplier) {
        RefreshableKey wrappedKey = getCastedString(key, RefreshableKey.class);
        if (Objects.isNull(wrappedKey)) {
            T newValue = supplier.get();
            if (Objects.isNull(newValue)) {
                log.info("[CacheService][Supplier null] Supplier returned null for key={}", key);
                return null;
            }
            String jsonString = JSON.toJSONString(newValue);
            setRefreshable(key, jsonString, refreshAfter, expireAfter);
            return jsonString;
        }

        if (wrappedKey.isUpForRefresh()) {
            String lockKey = String.format(REFRESH_LOCK_S, key);
            distributedLock.runWithLock(lockKey, 1, () -> refreshKeyAsync(key, refreshAfter, wrappedKey, supplier));
        }

        return wrappedKey.getValue();
    }

    @Override
    public <T> T getRefreshableFallback(String key, Class<T> clazz, Duration refreshAfter, Duration expireAfter, Supplier<T> supplier) {
        try {
            String value = getRefreshableFallback(key, refreshAfter, expireAfter, supplier);
            if (StringUtils.isNotEmpty(value)) {
                return JSON.parseObject(value, clazz);
            }
        } catch (Exception e) {
            log.warn("cast error", e);
        }
        return null;
    }

    @Override
    public void setRefreshable(String key, String value, Duration refreshAfter, Duration expireAfter) {
        RefreshableKey wrappedKey = RefreshableKey.builder()
                .value(value)
                .lastUpdatedTime(System.currentTimeMillis())
                .refreshAfter(refreshAfter)
                .build();

        set(key, JSON.toJSONString(wrappedKey), expireAfter);
    }

    @Override
    public void setRefreshable(String key, String value, Duration refreshAfter) {
        RefreshableKey wrappedKey = RefreshableKey.builder()
                .value(value)
                .lastUpdatedTime(System.currentTimeMillis())
                .refreshAfter(refreshAfter)
                .build();

        set(key, JSON.toJSONString(wrappedKey));
    }

    /**
     * Avoid refreshing synchronously unless you have good exception handling in place
     */
    private <T> void refreshKey(String key, Duration refreshAfter, RefreshableKey wrappedKey, Supplier<T> supplier) {
        Long expireAfter = getExpire(key);
        if (expireAfter <= 0) {
            log.info("[CacheService][Refresh][Expired] key={} expireAfter={}", key, expireAfter);
            return;
        }

        T value = supplier.get();

        if (Objects.isNull(value)) {
            log.info("[CacheService][Refresh][Supplier null] Supplier returned null for key={}", key);
            return;
        }

        String newJsonString = JSON.toJSONString(value);
        if (!newJsonString.equals(wrappedKey.getValue())) {
            log.info("[CacheService][Refresh][Difference] difference during refresh | before={}, after={}", wrappedKey.getValue(), newJsonString);
        }
        wrappedKey.setValue(newJsonString);
        wrappedKey.refreshUpdatedTime();
        // In case there is a new refreshAfter value, changes can take effect on next refresh, and not until the key expires
        wrappedKey.setRefreshAfter(refreshAfter);
        set(key, JSON.toJSONString(wrappedKey), Duration.ofSeconds(expireAfter));
    }

    private <T> void refreshKeyAsync(String key, Duration refreshAfter, RefreshableKey wrappedKey, Supplier<T> supplier) {
        log.info("[CacheService][Refresh Async] Refreshing key={}", key);
        CompletableFuture.runAsync(() -> refreshKey(key, refreshAfter, wrappedKey, supplier), c2cAsyncExecutor)
                .exceptionally(e -> {
                    log.error("[CacheService][Refresh Async] Exception refreshing key={}, wrappedKey={}, e={}", key, wrappedKey, e.getMessage());
                    return null;
                });
    }

    /**
     * Wrapper class for storing value as well as {@code lastUpdatedTime} for refresh mechanism.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static class RefreshableKey {
        /**
         * Can be a Json string
         */
        private String value;
        private long lastUpdatedTime;
        /**
         * Time (after lastUpdatedTime) when a key refresh will be attempted
         */
        private Duration refreshAfter;

        private boolean isUpForRefresh() {
            Duration durationSinceLastUpdate = Duration.ofMillis(System.currentTimeMillis() - lastUpdatedTime);
            return durationSinceLastUpdate.compareTo(refreshAfter) >= 0;
        }

        private void refreshUpdatedTime() {
            lastUpdatedTime = System.currentTimeMillis();
        }
    }
}
