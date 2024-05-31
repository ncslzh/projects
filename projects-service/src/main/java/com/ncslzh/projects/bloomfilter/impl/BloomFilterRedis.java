package com.ncslzh.projects.bloomfilter.impl;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.google.common.math.DoubleMath;
import com.ncslzh.projects.bloomfilter.BloomFilter;
import com.ncslzh.projects.bloomfilter.BloomFilterHelper;
import com.ncslzh.projects.bloomfilter.BloomFilterManager;
import com.ncslzh.projects.bloomfilter.BloomFilterStrategies;
import com.ncslzh.projects.bloomfilter.RedisBitSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * NON-NLS
 *
 * A Distributed Redis BloomFilter inspired by Guava and Baqend.
 * A Bloom filter offers an approximate containment test with one-sided error:
 * if it claims that an element is contained in it, this might be in error,
 * but if it claims that an element is <i>not</i> contained in it, then this is definitely true.
 *
 * <p>The false positive probability ({@code FPP}) of a Bloom filter is defined as the probability
 * that {@linkplain #mightContain(Object)} will erroneously return {@code true} for an object that
 * has not actually been put in the {@code BloomFilter}.
 *
 * <p>See {@link BloomFilterManager} for declaring BFs used in the project
 *
 * @param <T> the type of instances that the {@code BloomFilter} accepts
 * @author nicholas.leong
 */
@Slf4j
public class BloomFilterRedis<T> implements BloomFilter<T> {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisKeys keys;
    private final RedisBitSet bf;
    private final Funnel<? super T> funnel;
    private final int numHashFunctions;
    private final BloomFilter.Strategy strategy;

    private BloomFilterRedis(
            StringRedisTemplate stringRedisTemplate,
            RedisKeys keys,
            RedisBitSet bf,
            Funnel<? super T> funnel,
            int numHashFunctions,
            BloomFilter.Strategy strategy) {

        Preconditions.checkArgument(Objects.nonNull(stringRedisTemplate), "StringRedisTemplate is null");
        Preconditions.checkArgument(Objects.nonNull(keys), "RedisKeys is null");
        Preconditions.checkArgument(Objects.nonNull(bf), "RedisBitSet is null");
        Preconditions.checkArgument(Objects.nonNull(funnel), "Funnel is null");
        Preconditions.checkArgument(Objects.nonNull(strategy), "Strategy is null");
        Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions (%s) must be > 0", numHashFunctions);
        Preconditions.checkArgument(numHashFunctions <= 255, "numHashFunctions (%s) must be <= 255", numHashFunctions);

        this.stringRedisTemplate = stringRedisTemplate;
        this.keys = keys;
        this.bf = bf;
        this.funnel = funnel;
        this.numHashFunctions = numHashFunctions;
        this.strategy = strategy;
    }

    /**
     * Creates a {@link BloomFilterRedis} with the expected number of insertions and expected false
     * positive probability.
     *
     * <p>Note that overflowing a {@code BloomFilterRedis} with significantly more elements than specified,
     * will result in its saturation, and a sharp deterioration of its false positive probability.
     *
     * @param funnel                   the funnel of T's that the constructed {@code BloomFilter} will use
     * @param expectedInsertions       the number of expected insertions to the constructed {@code
     *                                 BloomFilterRedis}; must be positive
     * @param falsePositiveProbability the desired false positive probability (must be positive and less than 1.0)
     * @param strategy                 the hashing strategy to be used for the constructed {@code BloomFilterRedis}
     * @return a {@code BloomFilterRedis}
     */
    static <T> BloomFilterRedis<T> create(
            StringRedisTemplate stringRedisTemplate,
            String name,
            long expectedInsertions,
            double falsePositiveProbability,
            Funnel<? super T> funnel,
            BloomFilter.Strategy strategy) {

        Preconditions.checkArgument(Objects.nonNull(stringRedisTemplate), "StringRedisTemplate is null");
        Preconditions.checkArgument(Objects.nonNull(name), "Name is null");
        Preconditions.checkArgument(Objects.nonNull(funnel), "Funnel is null");
        Preconditions.checkArgument(Objects.nonNull(strategy), "Strategy is null");
        Preconditions.checkArgument(expectedInsertions > 0, "Expected insertions (%s) must be > 0", expectedInsertions);
        Preconditions.checkArgument(falsePositiveProbability > 0.0, "False positive probability (%s) must be > 0.0", falsePositiveProbability);
        Preconditions.checkArgument(falsePositiveProbability < 1.0, "False positive probability (%s) must be < 1.0", falsePositiveProbability);

        long numBits = BloomFilterHelper.optimalNumOfBits(expectedInsertions, falsePositiveProbability);
        int numHashFunctions = BloomFilterHelper.optimalNumOfHashFunctions(expectedInsertions, numBits);
        log.info("[BloomFilterRedis] creating {}, numBits{}, numHashFunctions={}", name, numBits, numHashFunctions);

        RedisKeys keys = new RedisKeys(name);
        RedisBitSet redisBitSet = new RedisBitSet(stringRedisTemplate, keys.bitsKey, numBits);

        return new BloomFilterRedis<>(stringRedisTemplate, keys, redisBitSet, funnel, numHashFunctions, strategy);
    }

    public static <T> BloomFilterRedis<T> create(
            StringRedisTemplate stringRedisTemplate,
            String keyName,
            long expectedInsertions,
            double falsePositiveProbability,
            Funnel<? super T> funnel) {

        return create(stringRedisTemplate, keyName, expectedInsertions, falsePositiveProbability, funnel, BloomFilterStrategies.MURMUR128_MITZ_64);
    }

    @Override
    public boolean mightContain(T object) {
        return bf.isAllSet(strategy.hash(object, funnel, numHashFunctions, bf.getSize()));
    }

    @Override
    public boolean add(T object) {
        boolean hasSetNewBits = bf.setAll(strategy.hash(object, funnel, numHashFunctions, bf.getSize()));
        if (hasSetNewBits) {
            // Check concurrency for same object
            incrementUniqueInsertionCount();
        }
        return hasSetNewBits;
    }

    /**
     * For faster performance in inserting large volumes of objects, consider refactoring to use a single redis round-trip
     */
    @Override
    public List<Boolean> putAll(Collection<T> objects) {
        return objects.stream().map(this::add).toList();
    }

    @Override
    public double expectedFpp() {
        return Math.pow((double) bf.cardinality() / bf.getSize(), numHashFunctions);
    }

    @Override
    public long approximateElementCount() {
        long bitSize = bf.getSize();
        long bitCount = bf.cardinality();

        /**
         * Each insertion is expected to reduce the # of clear bits by a factor of
         * `numHashFunctions/bitSize`. So, after n insertions, expected bitCount is `bitSize * (1 - (1 -
         * numHashFunctions/bitSize)^n)`. Solving that for n, and approximating `ln x` as `x - 1` when x
         * is close to 1 (why?), gives the following formula.
         */
        double fractionOfBitsSet = (double) bitCount / bitSize;
        long estimate = DoubleMath.roundToLong(
                -Math.log1p(-fractionOfBitsSet) * bitSize / numHashFunctions, RoundingMode.HALF_UP);

        String redisCount = stringRedisTemplate.opsForValue().get(keys.countsKey);

        log.info("[BloomFilterRedis][ApproxCount] bitSize={}, bitCount={}, estimate={}, redisCount={}",
                bitSize, bitCount,  estimate, redisCount);

        return estimate;
    }

    @Override
    public String toString() {
        return bf.asBitSet().toString();
    }

    /**
     * For large bitSets, allocating memory in advance in prevents dynamic resizing that can affect Redis performance as it grows.
     *
     * <p>This function unfortunately is unstable if called during the creation of {@code BloomFilterRedis}.
     * Therefore, it should be called after BloomFilterRedis has been created
     */
    public void allocateMemoryInRedis() {
        log.info("[BloomFilterRedis] Trying to allocate memory for name={}, size={}", bf.getName(), bf.getSize());
        if (!stringRedisTemplate.hasKey(bf.getName())) {
            log.info("[BloomFilterRedis] Allocating");
            stringRedisTemplate.execute(
                    (RedisConnection connection) -> {
                        connection.setBit(bf.getName().getBytes(), bf.getSize() - 1, false);
                        return null;
                    });
        }
    }

    public void deleteFromRedis() {
        log.info("[BloomFilterRedis] Deleting name={} with size={} from redis", bf.getName(), bf.getSize());
        bf.clearAll();
        stringRedisTemplate.delete(keys.countsKey);
    }

    /**
     * Experimental: May not be as accurate as using the math from {@code approximateElementCount}
     */
    private void incrementUniqueInsertionCount() {
        stringRedisTemplate.opsForValue().increment(keys.countsKey);
    }


    static class RedisKeys {
        public final String bitsKey;

        // To store (estimated) number of unique insertions
        public final String countsKey;

        public RedisKeys(String name) {
            this.bitsKey = name + ":bits";
            this.countsKey = name + ":counts";
        }
    }
}