package io.mangoo.providers;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import io.mangoo.cache.Cache;
import io.mangoo.cache.CacheImpl;
import io.mangoo.configuration.Config;
import io.mangoo.enums.CacheName;

/**
 * 
 * @author svenkubiak
 *
 */
@Singleton
public class CacheProvider implements Provider<Cache> {
    private Map<String, Cache> caches = new HashMap<>();
    private CacheManager cacheManager;
    private Cache cache;

    @Inject
    public CacheProvider(Config config) {
        Objects.requireNonNull(config, "config can not be null");
        
        if (config.isClusteredCached()) {
            CacheManagerBuilder<PersistentCacheManager> clusteredCacheManagerBuilder = CacheManagerBuilder.newCacheManagerBuilder() 
                    .with(ClusteringServiceConfigurationBuilder.cluster(URI.create(config.getCacheClusterUrl())) 
                    .autoCreate());

            this.cacheManager = clusteredCacheManagerBuilder.build(true);
        } else {
            this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
            this.cacheManager.init();
        }

        initApplicationCache();
        initAuthenticationCache();
        initRequestCache();
        initServerEventCache();
        initWebSocketCache();
    }

    private void initApplicationCache() {
        CacheConfiguration<String, Object> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(20000))
                .build();

        this.cache = registerCacheConfiguration(CacheName.APPLICATION.toString(), configuration);
    }

    private void initAuthenticationCache() {
        CacheConfiguration<String, Object> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(20000))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(60, TimeUnit.MINUTES)))
                .build();

        registerCacheConfiguration(CacheName.AUTH.toString(), configuration);
    }

    private void initRequestCache() {
        CacheConfiguration<String, Object> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(40000))
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(60, TimeUnit.SECONDS)))
                .build();

        registerCacheConfiguration(CacheName.REQUEST.toString(), configuration);
    }

    private void initServerEventCache() {
        CacheConfiguration<String, Object> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(20000))
                .withExpiry(Expirations.timeToIdleExpiration(Duration.of(30, TimeUnit.MINUTES)))
                .build();

        registerCacheConfiguration(CacheName.SSE.toString(), configuration);
    }

    private void initWebSocketCache() {
        CacheConfiguration<String, Object> configuration = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(20000))
                .withExpiry(Expirations.timeToIdleExpiration(Duration.of(30, TimeUnit.MINUTES)))
                .build();

        registerCacheConfiguration(CacheName.WSS.toString(), configuration);
    }

    public Cache registerCacheConfiguration(String name, CacheConfiguration<String, Object> configuration) {
        cache = new CacheImpl(cacheManager.createCache(name, configuration));
        this.caches.put(name, cache);

        return cache;
    }

    @Override
    public Cache get() {
        return this.cache;
    }

    /**
     * Retrieves a cache by its name from the cache pool
     * 
     * @param name The name of the cache
     * @return An Cache instance
     */
    public Cache getCache(CacheName name) {
        return getCache(name.toString());
    }

    /**
     * Retrieves a cache by its name from the cache pool
     * 
     * @param name The name of the cache
     * @return An Cache instance
     */
    public Cache getCache(String name) {
        return this.caches.get(name);
    }

    /**
     * Closes all caches
     */
    public void close() {
        cacheManager.close();
    }
}