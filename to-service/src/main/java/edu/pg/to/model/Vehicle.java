package edu.pg.to.model;

import edu.pg.to.dto.VehicleDto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String vehicleId;
    private Date startDate;
    private Date endDate;
    @ManyToOne
    private Offer offer;

    public VehicleDto toDto() {
        return new VehicleDto(
                vehicleId, startDate, endDate
        );
    }
}
