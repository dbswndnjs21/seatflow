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

@Entity
@Table(
    name = "seat",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_train_car_seat_no", columnNames = {"train_car_id", "seat_no"})
    }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_car_id", nullable = false)
    private TrainCar trainCar;

    @Column(name = "seat_no", nullable = false, length = 20)
    private String seatNo;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType = "NORMAL";

    protected Seat() {
    }
}
