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

@Entity
@Table(
    name = "reservation_passenger",
    indexes = {
        @Index(name = "idx_reservation_passenger_reservation", columnList = "reservation_id")
    }
)
public class ReservationPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "passenger_type", nullable = false, length = 20)
    private String passengerType;

    @Column(name = "passenger_name", nullable = false, length = 80)
    private String passengerName;

    protected ReservationPassenger() {
    }
}
