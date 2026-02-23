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
    name = "train_car",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_train_car_train_car_no", columnNames = {"train_id", "car_no"})
    }
)
public class TrainCar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "car_no", nullable = false)
    private int carNo;

    @Column(name = "class_type", nullable = false, length = 20)
    private String classType;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    protected TrainCar() {
    }
}
