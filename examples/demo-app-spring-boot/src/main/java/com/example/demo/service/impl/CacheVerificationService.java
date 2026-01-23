package com.example.demo.service.impl;

import com.example.demo.repository.VerificationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service responsible for cache-related verification operations.
 * Tests Spring Cache instrumentation with @Cacheable and @CacheEvict
 * annotations.
 */
@Service
public class CacheVerificationService {

    private final VerificationRepository repository;

    public CacheVerificationService(VerificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Verifies cache operations by performing a cache population and cache hit.
     * First call populates the cache, second call hits the cache.
     * 
     * @return Verification result message
     */
    @CacheEvict(value = "dbCount", allEntries = true)
    public String verifyCacheHit() {
        // First call should populate the cache
        String firstCall = verifyDbRead();
        // Second call should hit the cache
        String secondCall = verifyDbRead();
        return "Cache Check Result: First call: " + firstCall + ", Second call (cached): " + secondCall;
    }

    /**
     * Cacheable method that reads from the database.
     * This method's results are cached with key 'count'.
     * 
     * @return DB read result message
     */
    @Cacheable(value = "dbCount", key = "'count'")
    public String verifyDbRead() {
        long count = repository.count();
        return "DB Check Result: Count=" + count;
    }

    /**
     * Clears all cache entries.
     */
    @CacheEvict(value = "dbCount", allEntries = true)
    public void clearCache() {
        // Cache eviction happens automatically via annotation
    }
}
