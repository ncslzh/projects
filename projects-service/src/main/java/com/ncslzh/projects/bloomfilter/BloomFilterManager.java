package com.ncslzh.projects.bloomfilter;

import com.google.common.hash.Funnels;
import com.ncslzh.projects.bloomfilter.impl.BloomFilterRedis;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * A class for defining BloomFilter as beans so that they can be easily injected into any other Spring managed beans
 */
@Configuration
@Slf4j
public class BloomFilterManager {

//    @Qualifier("StringRedisTemplate")
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Bean
    @Qualifier(BeanName.USER_ID)
    public BloomFilterRedis<Long> userIdBloomFilter() {
        // Values of expectedInsertions and fpp should NOT be changed once BF is in redis
        return BloomFilterRedis.create(
                stringRedisTemplate, KeyPrefix.USER_ID, 70_000_000, 0.00001, Funnels.longFunnel());
    }

    public static class KeyPrefix {
        public static final String USER_ID = "otc:bf-userId";
    }

    public static class BeanName {
        public static final String USER_ID = "userIdBloomFilter";
    }
}
