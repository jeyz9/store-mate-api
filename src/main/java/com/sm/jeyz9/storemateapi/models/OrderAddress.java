package com.sm.jeyz9.storemateapi.models;

import jakarta.persistence.Entity;
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
@Table(name = "order_address")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;

    private String streetAddress;

    @ManyToOne
    @JoinColumn(name = "zipcode_id", referencedColumnName = "id")
    private Zipcode zipcode;

    private LocalDateTime createdAt = LocalDateTime.now();
}
