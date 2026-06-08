package com.nisarg.services.contracts;

import com.nisarg.dtos.CachedEventPageDTO;

import java.util.Optional;

public interface EventCacheStore {

    Optional<CachedEventPageDTO> getCachedEventPage(int page, int size);

    void cacheEventPage(CachedEventPageDTO eventPage);

    void deleteEventPagebyKey(int page, int size);

    void deleteAllEventPage();
}
