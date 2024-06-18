package com.ncslzh.projects.bloomfilter;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * Persistent BitSet stored in Redis inspired by Guava and Baqend
 *
 * <p>Exercise caution when declaring a large BitSet, as it may severely degrade Redis Performance
 * <p>Each call to any operations uses at most one round-trip by taking advantage of pipelining.
 */
@Getter
@Data
@Slf4j
public class RedisBitSet {

    /**
     *  Using stringRedisTemplate is ok, since Redis stores bits as Strings
     */
    private final StringRedisTemplate stringRedisTemplate;

    private final String name;

    /**
     * Number of bits
     */
    private final long size;


    public RedisBitSet(StringRedisTemplate stringRedisTemplate, String name, long size) {
        Preconditions.checkArgument(Objects.nonNull(stringRedisTemplate), "StringRedisTemplate is null");
        Preconditions.checkArgument(Objects.nonNull(name), "Name is null");
        Preconditions.checkArgument(size > 0, "RedisBitSet size should be > 0");

        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
        this.size = size;
    }

    public boolean get(long bitIndex) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().getBit(name, bitIndex));
    }

    /**
     * Should only be used in a pipelined operation
     */
    private void get(RedisConnection connection, long position) {
        connection.getBit(name.getBytes(), position);
    }

    /**
     * Fetches the values at the given index positions.
     *
     * @param indexes the index positions to query
     * @return an array containing the values at the given index positions
     */
    public Boolean[] getAll(long... indexes) {
        List<Object> results = stringRedisTemplate.executePipelined(
                (RedisConnection connection) -> {
                    for (long index : indexes) {
                        get(connection, index);
                    }
                    return null;
                });

        return results.stream().map(b -> (Boolean) b).toArray(Boolean[]::new);
    }

    /**
     * @return original value of the bit at bitIndex
     */
    public boolean set(long bitIndex, boolean value) {
        return stringRedisTemplate.opsForValue().setBit(name, bitIndex, value);
    }

    public boolean set(long bitIndex) {
        return set(bitIndex, true);
    }

    /**
     * Should only be used in a pipelined operation
     */
    private void set(RedisConnection connection, long bitIndex, boolean value) {
        connection.setBit(name.getBytes(), bitIndex, value);
    }

    /**
     * Tests whether the provided bit positions are all set.
     *
     * @param positions the positions to test
     * @return <code>true</code> if all positions are set
     */
    public boolean isAllSet(long... positions) {
        Boolean[] results = getAll(positions);
        return Stream.of(results).allMatch(b -> b);
    }

    /**
     * Set all bits
     *
     * @param positions The positions to set
     * @return {@code true} if any of the bits was previously unset.
     */
    public boolean setAll(long... positions) {
        List<Object> results = stringRedisTemplate.executePipelined(
                (RedisConnection connection) -> {
                    for (long position : positions) {
                        set(connection, position, true);
                    }
                    return null;
                }
        );

        return results.stream().anyMatch(b -> !(Boolean) b);
    }

    public void clear(long bitIndex) {
        set(bitIndex, false);
    }

    public void clear(RedisConnection connection, long bitIndex) {
        set(connection, bitIndex, false);
    }

    /**
     * WARNING: Deletes whole BF from redis
     */
    public void clearAll() {
        stringRedisTemplate.delete(name);
    }


    /**
     * Returns the number of bits set to true in this BitSet.
     *
     * <p>WARNING: Avoid calling this function in high frequency especially for large BitSets,
     * as this can get expensive really quick, and slow down Redis considerably
     *
     * @return the number of bits set to true in this BitSet.
     */
    public long cardinality() {
        return stringRedisTemplate.execute((RedisConnection connection) -> connection.bitCount(name.getBytes()));
    }

    /**
     * WARNING: Evaluate before calling this function for large BitSets, as this will
     * <li>
     *   1. Eat up a huge chunk of the heap memory of the calling machine
     *   2. Cause Redis to slow down drastically
     * </li>
     */
    public byte[] toByteArray() {
        byte[] bytes = stringRedisTemplate.execute((RedisConnection connection) ->
                connection.get(name.getBytes()));

        // If the key doesn't exist, return an appropriately sized byte array
        return bytes == null
                ? new byte[(int) Math.ceil(size / 8.0)]
                : bytes;
    }

    /**
     * WARNING: Will not work for when there are more than Integer.MAX_SIZE bits as BitSet size is of type int
     * Redis probably would not like sending Integer.MAX_SIZE bits (~268_435_456B / 268MB) over anyway
     */
    public BitSet asBitSet() {
        return BitSet.valueOf(toByteArray());
    }
}
