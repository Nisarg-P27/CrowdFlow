package com.nisarg.infrastructure;

import com.nisarg.dtos.CachedEventPageDTO;
import com.nisarg.services.contracts.EventCacheStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisEventCacheStore implements EventCacheStore {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String EVENT_PAGE_PREFIX = "events:";
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    public Optional<CachedEventPageDTO> getCachedEventPage(int page, int size) {
//        return Optional.empty();
            CachedEventPageDTO cachedEventPage = (CachedEventPageDTO) redisTemplate.opsForValue()
                    .get(buildPageKey(page, size));

            return Optional.ofNullable(cachedEventPage);

    }

    @Override
    public void cacheEventPage(CachedEventPageDTO eventPage) {
            try {
        redisTemplate.opsForValue()
                .set(buildPageKey(eventPage.page(), eventPage.size()), eventPage, TTL);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteEventPagebyKey(int page, int size) {
        redisTemplate.delete(buildPageKey(page, size));
    }

    @Override
    public void deleteAllEventPage() {
        Set<String> keys = redisTemplate.keys(EVENT_PAGE_PREFIX + "*");

        if(keys!=null && !keys.isEmpty()){
            redisTemplate.delete(keys);
        }
    }

    private String buildPageKey(
            int page,
            int size
    ) {

        return EVENT_PAGE_PREFIX
                + "page:" + page
                + ":size:" + size;
    }
}
