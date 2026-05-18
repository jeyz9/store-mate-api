package com.sm.jeyz9.storemateapi.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    
    @Enumerated(EnumType.STRING)
    private RefundStatusName status;
    
    @Column(nullable = false)
    private String reason;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDateTime approvedAt;
    private LocalDateTime requestedAt;
    private String refundNo;
}
