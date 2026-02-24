package com.seatflow.ktx.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.hold.expire.enabled", havingValue = "true", matchIfMissing = true)
public class SeatHoldExpireScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldExpireScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public SeatHoldExpireScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${app.hold.expire.fixed-delay-ms:5000}")
    public void releaseExpiredHolds() {
        int released = jdbcTemplate.update(
            """
            UPDATE seat_inventory
            SET availability_status = 'AVAILABLE',
                hold_token = NULL,
                hold_expires_at = NULL,
                updated_at = NOW(3)
            WHERE availability_status = 'HELD'
              AND hold_expires_at IS NOT NULL
              AND hold_expires_at <= NOW(3)
            """
        );

        if (released > 0) {
            log.info("released expired seat holds: {}", released);
        }
    }
}
