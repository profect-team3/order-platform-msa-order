package app.domain.order.kafka.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import app.domain.order.kafka.Outbox;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

	// PENDING 상태 레코드 가져오기 (native는 그대로 사용 가능)
	@Query(value = """
        SELECT * FROM outbox
         WHERE status = 'PENDING'
         ORDER BY created_at
         FOR UPDATE SKIP LOCKED
         LIMIT :limit
        """, nativeQuery = true)
	List<Outbox> fetchPendingForUpdate(@Param("limit") int limit);

	// SENT 처리
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
        UPDATE Outbox e
           SET e.status = app.domain.order.kafka.Outbox.Status.SENT,
               e.updatedAt = :now,
               e.lastError = null
         WHERE e.id = :id
        """)
	int markSent(@Param("id") UUID id, @Param("now") LocalDateTime now);

	// FAILED 처리
	@Modifying
	@Query("""
        UPDATE Outbox e
           SET e.status = app.domain.order.kafka.Outbox.Status.FAILED,
               e.updatedAt = :now,
               e.lastError = :err
         WHERE e.id = :id
        """)
	int markFailed(@Param("id") UUID id, @Param("now") LocalDateTime now, @Param("err") String err);

	// 오래된 FAILED → PENDING 재큐잉
	@Modifying
	@Query("""
        UPDATE Outbox e
           SET e.status = app.domain.order.kafka.Outbox.Status.PENDING,
               e.updatedAt = :now,
               e.lastError = null
         WHERE e.status = app.domain.order.kafka.Outbox.Status.FAILED
           AND e.updatedAt < :retryBefore
        """)
	int requeueFailed(@Param("now") LocalDateTime now, @Param("retryBefore") LocalDateTime retryBefore);
}