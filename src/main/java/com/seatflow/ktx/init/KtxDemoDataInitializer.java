package com.seatflow.ktx.init;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.init.demo.enabled", havingValue = "true", matchIfMissing = false)
public class KtxDemoDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public KtxDemoDataInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer runCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM train_run", Integer.class);
        if (runCount != null && runCount > 0) {
            return;
        }

        seedTrainAndRuns();
    }

    private void seedTrainAndRuns() {
        List<TrainSeed> seeds = List.of(
            new TrainSeed("KTX001", "KTX", "서울", "부산", 2, 30),
            new TrainSeed("KTX101", "KTX", "서울", "강릉", 1, 50),
            new TrainSeed("KTX201", "KTX", "용산", "목포", 2, 20),
            new TrainSeed("KTX301", "KTX", "행신", "부산", 2, 40),
            new TrainSeed("KTX401", "KTX", "청량리", "안동", 2, 10)
        );

        for (TrainSeed seed : seeds) {
            Long trainId = insertTrain(seed.trainNo(), seed.trainType());
            seedCarsAndSeats(trainId);
            seedRuns(trainId, seed);
        }
    }

    private Long insertTrain(String trainNo, String trainType) {
        jdbcTemplate.update(
            "INSERT INTO train (train_no, train_type, active, created_at) VALUES (?, ?, ?, ?)",
            trainNo,
            trainType,
            true,
            Timestamp.valueOf(LocalDateTime.now())
        );
        return jdbcTemplate.queryForObject("SELECT id FROM train WHERE train_no = ?", Long.class, trainNo);
    }

    private void seedCarsAndSeats(Long trainId) {
        for (int carNo = 1; carNo <= 4; carNo++) {
            String classType = carNo == 1 ? "FIRST" : "STANDARD";
            jdbcTemplate.update(
                "INSERT INTO train_car (train_id, car_no, class_type, seat_count) VALUES (?, ?, ?, ?)",
                trainId,
                carNo,
                classType,
                20
            );

            Long trainCarId = jdbcTemplate.queryForObject(
                "SELECT id FROM train_car WHERE train_id = ? AND car_no = ?",
                Long.class,
                trainId,
                carNo
            );

            for (int row = 1; row <= 5; row++) {
                for (char col : new char[]{'A', 'B', 'C', 'D'}) {
                    String seatNo = row + String.valueOf(col);
                    jdbcTemplate.update(
                        "INSERT INTO seat (train_car_id, seat_no, seat_type) VALUES (?, ?, ?)",
                        trainCarId,
                        seatNo,
                        "NORMAL"
                    );
                }
            }
        }
    }

    private void seedRuns(Long trainId, TrainSeed seed) {
        Long departureStationId = findStationId(seed.departureStation());
        Long arrivalStationId = findStationId(seed.arrivalStation());

        List<LocalTime> departures = List.of(LocalTime.of(8, 0), LocalTime.of(12, 0), LocalTime.of(16, 0));
        for (int dayOffset = 0; dayOffset < 3; dayOffset++) {
            LocalDate date = LocalDate.now().plusDays(dayOffset);

            for (LocalTime departureTime : departures) {
                LocalDateTime departure = LocalDateTime.of(date, departureTime);
                LocalDateTime arrival = departure.plusHours(seed.durationHours()).plusMinutes(seed.durationMinutes());

                jdbcTemplate.update(
                    "INSERT INTO train_run (train_id, departure_station_id, arrival_station_id, departure_time, arrival_time, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    trainId,
                    departureStationId,
                    arrivalStationId,
                    Timestamp.valueOf(departure),
                    Timestamp.valueOf(arrival),
                    "OPEN",
                    Timestamp.valueOf(LocalDateTime.now())
                );

                jdbcTemplate.update(
                    "INSERT INTO train_run (train_id, departure_station_id, arrival_station_id, departure_time, arrival_time, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    trainId,
                    arrivalStationId,
                    departureStationId,
                    Timestamp.valueOf(departure.plusHours(1)),
                    Timestamp.valueOf(arrival.plusHours(1)),
                    "OPEN",
                    Timestamp.valueOf(LocalDateTime.now())
                );
            }
        }
    }

    private Long findStationId(String stationName) {
        Long stationId = jdbcTemplate.queryForObject("SELECT id FROM station WHERE name = ?", Long.class, stationName);
        if (stationId == null) {
            throw new IllegalStateException("역 데이터가 없습니다: " + stationName);
        }
        return stationId;
    }

    private record TrainSeed(
        String trainNo,
        String trainType,
        String departureStation,
        String arrivalStation,
        int durationHours,
        int durationMinutes
    ) {
    }
}
