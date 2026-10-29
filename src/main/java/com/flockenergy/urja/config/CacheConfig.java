package com.flockenergy.urja.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    private final UrjaPortalProperties properties;

    public CacheConfig(UrjaPortalProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.getCache().getTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(properties.getCache().getMaxSize())
                .recordStats());
        return cacheManager;
    }
}
