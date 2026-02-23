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
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payment_attempt",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_attempt_no", columnNames = {"payment_id", "attempt_no"})
    },
    indexes = {
        @Index(name = "idx_payment_attempt_created", columnList = "created_at")
    }
)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "result_status", nullable = false, length = 30)
    private String resultStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PaymentAttempt() {
    }
}
