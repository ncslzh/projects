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
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Component
@Slf4j
public class RefreshableCacheServiceImpl extends CacheServiceImpl implements RefreshableCacheService {
    @Resource private Executor asyncExecutor;

    private static final String REFRESH_LOCK_S = "refreshKey_%s";

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
        CompletableFuture.runAsync(() -> refreshKey(key, refreshAfter, wrappedKey, supplier), asyncExecutor)
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
