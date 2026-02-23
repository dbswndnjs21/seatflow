package com.seatflow.station.api;

import com.seatflow.station.domain.Station;
import com.seatflow.station.service.StationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public List<StationResponse> getStations(@RequestParam(name = "q", required = false) String query) {
        return stationService.findStations(query)
            .stream()
            .map(StationResponse::from)
            .toList();
    }

    public record StationResponse(Long id, String name) {
        static StationResponse from(Station station) {
            return new StationResponse(station.getId(), station.getName());
        }
    }
}
