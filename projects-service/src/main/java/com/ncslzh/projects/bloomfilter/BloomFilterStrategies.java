package com.ncslzh.projects.bloomfilter;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import com.ncslzh.projects.bloomfilter.BloomFilter;

/**
 * Collections of strategies of generating the k * log(M) bits required for an element to be mapped
 * to a BloomFilter of M bits and k hash functions. These strategies are part of the Bloom filters that use them,
 * thus they must be preserved as is (no updates allowed, only introduction of new versions).
 *
 * The default go-to strategy would be MURMUR128_MITZ_64, unless there are new studies to show significant improvement
 * in newer (hashing) strategies, or if we happen to need the 32 bit version.
 *
 * @author nicholas.leong
 */
public enum BloomFilterStrategies implements BloomFilter.Strategy {

    /**
     * This strategy uses all 128 bits of {@link Hashing#murmur3_128} when hashing. It looks different
     * from the Guava implementation of MURMUR128_MITZ_32 because we're avoiding the multiplication in the
     * loop and doing a (much simpler) += hash2. We're also changing the index to a positive number by
     * AND'ing with Long.MAX_VALUE instead of flipping the bits.
     */
    MURMUR128_MITZ_64() {
        @Override
        public <T> long[] hash(T object, Funnel<? super T> funnel, int numHashFunctions, long bloomFilterBitSize) {
            long[] result = new long[numHashFunctions];
            byte[] bytes = Hashing.murmur3_128().hashObject(object, funnel).asBytes();
            long hash1 = lowerEight(bytes);
            long hash2 = upperEight(bytes);

            long combinedHash = hash1;
            for (int i = 0; i < numHashFunctions; i++) {
                // Make the combined hash positive and index-able
                result[i] = (int) ((combinedHash & Long.MAX_VALUE) % bloomFilterBitSize);
                combinedHash += hash2;
            }

            return result;
        }

        private long lowerEight(byte[] bytes) {
            return Longs.fromBytes(
                    bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        }

        private long upperEight(byte[] bytes) {
            return Longs.fromBytes(
                    bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
        }
    }
}
