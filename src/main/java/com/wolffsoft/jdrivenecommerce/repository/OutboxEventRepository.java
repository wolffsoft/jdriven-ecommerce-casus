package com.wolffsoft.jdrivenecommerce.repository;

import com.wolffsoft.jdrivenecommerce.repository.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
        SELECT id
        FROM outbox_event
        WHERE status IN ('NEW','FAILED')
          AND next_attempt_at <= now()
        ORDER BY created_at
        LIMIT :batchSize
        """, nativeQuery = true)
    List<UUID> findReadyIds(@Param("batchSize") int batchSize);

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'IN_PROGRESS',
            locked_by = :lockedBy,
            locked_at = :lockedAt
        WHERE id IN (:ids)
          AND status IN ('NEW','FAILED')
          AND next_attempt_at <= now()
        """, nativeQuery = true)
    void claimIds(@Param("ids") List<UUID> ids,
                 @Param("lockedBy") String lockedBy,
                 @Param("lockedAt") Instant lockedAt);

    @Query(value = """
        SELECT *
        FROM outbox_event
        WHERE status = 'IN_PROGRESS'
          AND locked_by = :lockedBy
        ORDER BY created_at
        """, nativeQuery = true)
    List<OutboxEventEntity> findClaimed(@Param("lockedBy") String lockedBy);

    @Modifying
    @Query(value = """
    UPDATE outbox_event
    SET status = 'PUBLISHED',
        published_at = :publishedAt,
        publish_attempts = :attempts,
        locked_by = null,
        locked_at = null,
        last_error = null
    WHERE id = :id
    """, nativeQuery = true)
    void markPublished(@Param("id") UUID id,
                      @Param("attempts") int attempts,
                      @Param("publishedAt") Instant publishedAt);

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'FAILED',
            publish_attempts = :attempts,
            next_attempt_at = :nextAttemptAt,
            last_error = :lastError,
            locked_by = null,
            locked_at = null
        WHERE id = :id
        """, nativeQuery = true)
    void markFailed(@Param("id") UUID id,
                   @Param("attempts") int attempts,
                   @Param("nextAttemptAt") Instant nextAttemptAt,
                   @Param("lastError") String lastError);

    @Modifying
    @Query(value = """
        UPDATE outbox_event
        SET status = 'FAILED',
            next_attempt_at = now(),
            last_error = 'Recovered from stale lock',
            locked_by = null,
            locked_at = null
        WHERE status = 'IN_PROGRESS'
          AND locked_at IS NOT NULL
          AND locked_at < (now() - (:maxAgeSeconds || ' seconds')::interval)
        """, nativeQuery = true)
    int recoverStaleLocks(@Param("maxAgeSeconds") long maxAgeSeconds);
}
