package com.seatflow.ktx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reservation_seat",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_seat_inventory", columnNames = {"seat_inventory_id"}),
        @UniqueConstraint(name = "uk_reservation_seat_unique", columnNames = {"reservation_id", "seat_inventory_id"})
    }
)
public class ReservationSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_inventory_id", nullable = false)
    private SeatInventory seatInventory;

    @Column(name = "fare_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fareAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ReservationSeat() {
    }
}
