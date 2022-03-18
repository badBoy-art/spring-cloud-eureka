package com.example.manager;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.data.redis.cache.RedisCache;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author xuedui.zhao
 * @create 2019-07-09
 * @see org.springframework.data.redis.cache.RedisCacheManager
 */
public class RedisCacheManager extends AbstractTransactionSupportingCacheManager {

    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();

        return caches;
    }


}
