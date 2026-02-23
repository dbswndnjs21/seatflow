package com.seatflow.ktx.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "seat_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_inventory_run_seat", columnNames = {"train_run_id", "seat_id"})
    },
    indexes = {
        @Index(name = "idx_seat_inventory_status_expire", columnList = "availability_status,hold_expires_at"),
        @Index(name = "idx_seat_inventory_hold_token", columnList = "hold_token")
    }
)
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_run_id", nullable = false)
    private TrainRun trainRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "availability_status", nullable = false, length = 20)
    private String availabilityStatus = "AVAILABLE";

    @Column(name = "hold_token", length = 36)
    private String holdToken;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "reserved_by_reservation_id")
    private Long reservedByReservationId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SeatInventory() {
    }
}
