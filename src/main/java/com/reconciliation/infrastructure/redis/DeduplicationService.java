package com.reconciliation.infrastructure.redis;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-backed deduplication service
 * <h2>Why Redis and not just the DB constraint?</h2>
 * <p>
 * The DB {@code UNIQUE} constraints on {@code external_id} prevents corrupt
 * data, but it doesn't prevent wasted work. Without deduplication, a restarted
 * job re-reads every CSV row, calls the use case, hits the writer, and only
 * then discovers (via unique constraint or the SELECT in the writer) that the
 * transaction was already processed. For a file with 500k rows where 490k were
 * already processed, that's 490k uncessary calls.
 * </p>
 */

@Service
@Slf4j
public class DeduplicationService {

    private static final String KEY_PREFIX = "recon:dedup:";

    private final StringRedisTemplate redisTemplate;
    private final Duration dedupTtl;

    public DeduplicationService(StringRedisTemplate redisTemplate, @Value("${app.batch.dedup-ttl}") int dedupTtlDays) {
        this.redisTemplate = redisTemplate;
        this.dedupTtl = Duration.ofDays(dedupTtlDays);
    }

    public boolean isDuplicate(String externalId, String runDate) {
        try {
            Boolean exists = redisTemplate.hasKey(buildKey(externalId, runDate));
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn(
                    "Redis unavailable during duplicate check for externalId={}. Treating as non-duplicate - DB constraint is the safety net.",
                    externalId, e);
            return false;
        }
    }

    public void markProcessed(String externalId, String runDate) {
        try {
            redisTemplate.opsForValue().set(buildKey(externalId, runDate), "1", dedupTtl);
        } catch (Exception e) {
            log.warn(
                    "Redis unavailable. Could not mark externalId={} as processed. Dedup key will be missing: DB constraint remains active.",
                    externalId, e);
        }
    }

    private String buildKey(String externalId, String runDate) {
        return KEY_PREFIX + runDate + ":" + externalId;
    }
}
