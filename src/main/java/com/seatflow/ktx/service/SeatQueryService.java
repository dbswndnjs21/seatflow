package com.seatflow.ktx.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeatQueryService {

    private final JdbcTemplate jdbcTemplate;

    public SeatQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SeatView> getSeats(Long runId) {
        ensureSeatInventoryRows(runId);

        String sql = """
            SELECT
                si.id AS seat_inventory_id,
                tc.car_no,
                tc.class_type,
                s.seat_no,
                si.availability_status,
                si.hold_expires_at
            FROM seat_inventory si
            INNER JOIN seat s ON s.id = si.seat_id
            INNER JOIN train_car tc ON tc.id = s.train_car_id
            WHERE si.train_run_id = ?
            ORDER BY tc.car_no, s.seat_no
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SeatView(
            rs.getLong("seat_inventory_id"),
            rs.getInt("car_no"),
            rs.getString("class_type"),
            rs.getString("seat_no"),
            rs.getString("availability_status"),
            rs.getTimestamp("hold_expires_at") == null ? null : rs.getTimestamp("hold_expires_at").toLocalDateTime()
        ), runId);
    }

    private void ensureSeatInventoryRows(Long runId) {
        String sql = """
            INSERT INTO seat_inventory (train_run_id, seat_id, availability_status, version, updated_at)
            SELECT ?, s.id, 'AVAILABLE', 0, NOW(3)
            FROM train_run tr
            INNER JOIN train_car tc ON tc.train_id = tr.train_id
            INNER JOIN seat s ON s.train_car_id = tc.id
            WHERE tr.id = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM seat_inventory si
                  WHERE si.train_run_id = ?
                    AND si.seat_id = s.id
              )
            """;

        jdbcTemplate.update(sql, runId, runId, runId);
    }

    public record SeatView(
        Long seatInventoryId,
        int carNo,
        String classType,
        String seatNo,
        String status,
        LocalDateTime holdExpiresAt
    ) {
    }
}
