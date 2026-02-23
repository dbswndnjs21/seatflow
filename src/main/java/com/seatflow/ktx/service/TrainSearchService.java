package com.seatflow.ktx.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TrainSearchService {

    private final JdbcTemplate jdbcTemplate;

    public TrainSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrainSearchResult> search(String fromStation, String toStation, LocalDateTime departureDateTime) {
        LocalDateTime end = departureDateTime.plusDays(1);

        String sql = """
            SELECT
                tr.id AS run_id,
                t.train_no,
                t.train_type,
                ds.name AS departure_station,
                ars.name AS arrival_station,
                tr.departure_time,
                tr.arrival_time,
                COALESCE(tc.total_seats, 0) AS total_seats
            FROM train_run tr
            INNER JOIN train t ON t.id = tr.train_id
            INNER JOIN station ds ON ds.id = tr.departure_station_id
            INNER JOIN station ars ON ars.id = tr.arrival_station_id
            LEFT JOIN (
                SELECT train_id, SUM(seat_count) AS total_seats
                FROM train_car
                GROUP BY train_id
            ) tc ON tc.train_id = t.id
            WHERE tr.status = 'OPEN'
              AND ds.name = ?
              AND ars.name = ?
              AND tr.departure_time >= ?
              AND tr.departure_time < ?
            ORDER BY tr.departure_time
            LIMIT 30
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                LocalDateTime departure = rs.getTimestamp("departure_time").toLocalDateTime();
                LocalDateTime arrival = rs.getTimestamp("arrival_time").toLocalDateTime();
                long durationMinutes = Duration.between(departure, arrival).toMinutes();

                int totalSeats = rs.getInt("total_seats");
                int availableSeats = Math.max(0, totalSeats - ((rowNum * 7) % Math.max(totalSeats, 1)));
                BigDecimal baseFare = BigDecimal.valueOf("KTX".equals(rs.getString("train_type")) ? 59000 : 32000);

                return new TrainSearchResult(
                    rs.getLong("run_id"),
                    rs.getString("train_no"),
                    rs.getString("train_type"),
                    rs.getString("departure_station"),
                    rs.getString("arrival_station"),
                    departure,
                    arrival,
                    durationMinutes,
                    availableSeats,
                    baseFare
                );
            },
            fromStation,
            toStation,
            Timestamp.valueOf(departureDateTime),
            Timestamp.valueOf(end)
        );
    }

    public record TrainSearchResult(
        Long runId,
        String trainNo,
        String trainType,
        String departureStation,
        String arrivalStation,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        long durationMinutes,
        int availableSeats,
        BigDecimal baseFare
    ) {
    }
}
