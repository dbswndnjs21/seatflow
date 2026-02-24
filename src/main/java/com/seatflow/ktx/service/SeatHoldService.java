package com.seatflow.ktx.service;

import com.seatflow.ktx.lock.RedisLockService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class SeatHoldService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisLockService redisLockService;
    private final long lockWaitMillis;
    private final int holdDurationMinutes;

    public SeatHoldService(
        JdbcTemplate jdbcTemplate,
        RedisLockService redisLockService,
        @Value("${app.hold.lock-wait-ms:200}") long lockWaitMillis,
        @Value("${app.hold.duration-minutes:5}") int holdDurationMinutes
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisLockService = redisLockService;
        this.lockWaitMillis = lockWaitMillis;
        this.holdDurationMinutes = holdDurationMinutes;
    }

    public HoldResult holdSeat(Long seatInventoryId, String loginId) {
        String lockKey = "seat:hold:" + seatInventoryId;

        boolean locked = redisLockService.tryLock(lockKey, lockWaitMillis);
        if (!locked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 다른 사용자가 처리 중인 좌석입니다.");
        }

        try {
            return holdSeatWithPessimisticLock(seatInventoryId, loginId);
        } finally {
            redisLockService.unlock(lockKey);
        }
    }

    @Transactional
    protected HoldResult holdSeatWithPessimisticLock(Long seatInventoryId, String loginId) {
        Map<String, Object> seatRow;
        try {
            seatRow = jdbcTemplate.queryForMap(
                "SELECT id, availability_status, hold_expires_at FROM seat_inventory WHERE id = ? FOR UPDATE",
                seatInventoryId
            );
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "좌석 정보를 찾을 수 없습니다.");
        }

        String status = String.valueOf(seatRow.get("availability_status"));
        LocalDateTime now = LocalDateTime.now();
        Timestamp holdExpiresTimestamp = (Timestamp) seatRow.get("hold_expires_at");
        LocalDateTime holdExpiresAt = holdExpiresTimestamp == null ? null : holdExpiresTimestamp.toLocalDateTime();

        if ("RESERVED".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약된 좌석입니다.");
        }

        if ("HELD".equalsIgnoreCase(status) && holdExpiresAt != null && holdExpiresAt.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 선점 중인 좌석입니다.");
        }

        LocalDateTime newHoldExpiresAt = now.plusMinutes(holdDurationMinutes);
        String holdToken = UUID.randomUUID().toString();

        jdbcTemplate.update(
            """
            UPDATE seat_inventory
            SET availability_status = 'HELD',
                hold_token = ?,
                hold_expires_at = ?,
                updated_at = NOW(3)
            WHERE id = ?
            """,
            holdToken,
            Timestamp.valueOf(newHoldExpiresAt),
            seatInventoryId
        );

        return new HoldResult(seatInventoryId, "HELD", newHoldExpiresAt, loginId);
    }

    public record HoldResult(
        Long seatInventoryId,
        String status,
        LocalDateTime holdExpiresAt,
        String holderLoginId
    ) {
    }
}
