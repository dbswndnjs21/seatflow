package com.seatflow.ktx.api;

import com.seatflow.ktx.service.TrainSearchService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trains")
public class TrainSearchController {

    private final TrainSearchService trainSearchService;

    public TrainSearchController(TrainSearchService trainSearchService) {
        this.trainSearchService = trainSearchService;
    }

    @GetMapping("/search")
    public List<TrainSearchResponse> search(
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        @RequestParam("departureDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureDateTime
    ) {
        return trainSearchService.search(from, to, departureDateTime)
            .stream()
            .map(TrainSearchResponse::from)
            .toList();
    }

    public record TrainSearchResponse(
        Long runId,
        String trainNo,
        String trainType,
        String departureStation,
        String arrivalStation,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        long durationMinutes,
        int availableSeats,
        String baseFare
    ) {
        static TrainSearchResponse from(TrainSearchService.TrainSearchResult result) {
            return new TrainSearchResponse(
                result.runId(),
                result.trainNo(),
                result.trainType(),
                result.departureStation(),
                result.arrivalStation(),
                result.departureTime(),
                result.arrivalTime(),
                result.durationMinutes(),
                result.availableSeats(),
                result.baseFare().toPlainString()
            );
        }
    }
}
