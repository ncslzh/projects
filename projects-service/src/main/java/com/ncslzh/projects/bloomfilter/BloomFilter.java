package com.ncslzh.projects.bloomfilter;

import com.google.common.hash.Funnel;

import java.util.Collection;
import java.util.List;

/**
 * Represents a Bloom filter and provides default methods for hashing.
 *
 * <p>Future plans:
 * <li>Make this interface (and implementations) compatible with {@code Cloneable} and {@code Serializable} interfaces
 * <li>Allow merging of two Bloom Filters
 *
 * @author nicholas.leong
 */
public interface BloomFilter<T> {

    /**
     * Tests whether an element is present in the filter (subject to the specified false positive rate).
     *
     * <p>Queries {@code numHashFunctions} bits of the given bit array, by hashing a user element.
     * <p>Returns {@code true} if and only if all selected bits are set.
     *
     * @param object to test
     * @return {@code true} if the element is contained
     */
    boolean mightContain(T object);

    /**
     * Adds the passed element to the filter.
     *
     * <p>Sets {@code numHashFunctions} bits of the given bit array, by hashing the passed element;
     * <p>Returns whether any bits changed as a result of this operation.
     *
     * @param object element to add
     * @return {@code true} if the value did not previously exist in the filter. Note, that a false positive may occur,
     * thus the value may not have already been in the filter, but it hashed to a set of bits already in the filter.
     */
    boolean add(T object);

    /**
     * Performs a bulk add operation for a collection of elements.
     *
     * @param objects to add
     * @return a list of booleans indicating for each element, whether it was previously present in the filter
     */
    List<Boolean> putAll(Collection<T> objects);

    /**
     * Returns the probability that {@linkplain #mightContain(Object)} will erroneously return {@code
     * true} for an object that has not actually been put in the {@code BloomFilterRedis}.
     *
     * <p>Ideally, this number should be close to the {@code fpp} parameter passed when creating the BF, or smaller.
     * If it is significantly higher, it is usually the case that too many elements (more than expected)
     * have been put in the {@code BloomFilter}, degenerating it.
     */
    double expectedFpp();

    /**
     * Returns an estimate for the total number of distinct elements that have been added to this
     * Bloom filter. This approximation is reasonably accurate if it does not exceed the value of
     * {@code expectedInsertions} that was used when constructing the filter.
     */
    long approximateElementCount();


    /**
     * A (hashing) strategy to translate T instances, to {@code numHashFunctions} bit indexes.
     *
     * <p>Implementations should be collections of pure functions (i.e. stateless).
     */
    interface Strategy {
        /**
         * Returns a long array of {@code numHashFunctions} bit-indexes that should be set in the bloom filter bit array.
         */
        <T> long[] hash(T object, Funnel<? super T> funnel, int numHashFunctions, long bloomFilterSize);
    }
}
