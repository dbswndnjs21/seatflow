package com.seatflow.ktx.api;

import com.seatflow.ktx.service.SeatHoldService;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seats")
public class SeatCommandController {

    private final SeatHoldService seatHoldService;

    public SeatCommandController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/{seatInventoryId}/hold")
    public HoldResponse hold(@PathVariable Long seatInventoryId, Authentication authentication) {
        SeatHoldService.HoldResult result = seatHoldService.holdSeat(seatInventoryId, authentication.getName());
        return new HoldResponse(
            result.seatInventoryId(),
            result.status(),
            result.holdExpiresAt(),
            result.holderLoginId()
        );
    }

    public record HoldResponse(
        Long seatInventoryId,
        String status,
        LocalDateTime holdExpiresAt,
        String holderLoginId
    ) {
    }
}
