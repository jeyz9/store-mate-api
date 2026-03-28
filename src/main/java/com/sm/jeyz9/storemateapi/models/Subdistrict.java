package com.sm.jeyz9.storemateapi.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subdistricts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subdistrict {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "district_id", referencedColumnName = "id")
    private District district;
    
    private String name;
    
    @Column(unique = true)
    private String subdistrictCode;
    
    @ManyToOne
    @JoinColumn(name = "geo_id", referencedColumnName = "id")
    private Geography geography;
    
    @ManyToOne
    @JoinColumn(name = "province_id", referencedColumnName = "id")
    private Province province;
}
