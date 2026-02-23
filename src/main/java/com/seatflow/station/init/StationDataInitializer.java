package com.seatflow.station.init;

import com.seatflow.station.service.StationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.init.station.enabled", havingValue = "true", matchIfMissing = false)
public class StationDataInitializer implements ApplicationRunner {

    private final StationService stationService;
    private final JdbcTemplate jdbcTemplate;

    public StationDataInitializer(StationService stationService, JdbcTemplate jdbcTemplate) {
        this.stationService = stationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureStationUtf8mb4();
        stationService.seedStationsIfEmpty();
    }

    private void ensureStationUtf8mb4() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS station (
                id BIGINT NOT NULL AUTO_INCREMENT,
                display_order INT NOT NULL,
                name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                PRIMARY KEY (id),
                UNIQUE KEY uk_station_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);

        jdbcTemplate.execute("ALTER TABLE station CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        jdbcTemplate.execute(
            "ALTER TABLE station MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL");
    }
}
