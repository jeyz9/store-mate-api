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

@Entity
@Table(name = "store_info")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String storeName;
    private String phone;
    private String streetAddress;
    private String email;
    private String promotionImage;
    
    @ManyToOne
    @JoinColumn(name = "zipcode_id", referencedColumnName = "id")
    private Zipcode zipcode;
}
