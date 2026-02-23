package com.seatflow.ktx.api;

import com.seatflow.ktx.service.SeatQueryService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/train-runs")
public class SeatQueryController {

    private final SeatQueryService seatQueryService;

    public SeatQueryController(SeatQueryService seatQueryService) {
        this.seatQueryService = seatQueryService;
    }

    @GetMapping("/{runId}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long runId) {
        return seatQueryService.getSeats(runId)
            .stream()
            .map(SeatResponse::from)
            .toList();
    }

    public record SeatResponse(
        Long seatInventoryId,
        int carNo,
        String classType,
        String seatNo,
        String status,
        LocalDateTime holdExpiresAt
    ) {
        static SeatResponse from(SeatQueryService.SeatView seatView) {
            return new SeatResponse(
                seatView.seatInventoryId(),
                seatView.carNo(),
                seatView.classType(),
                seatView.seatNo(),
                seatView.status(),
                seatView.holdExpiresAt()
            );
        }
    }
}
