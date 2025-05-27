package edu.pg.to.model;

import edu.pg.to.dto.OfferDto;
import edu.pg.to.dto.VehicleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String src;
    private String dest;
    private Date startTime;
    private Date endTime;
    private Double cost;
    private VehicleType type;
    private Integer maxSeats;
    @OneToMany(mappedBy = "offer", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<Vehicle> vehicles;

    public OfferDto toDto() {
        return new OfferDto(
                id,
                src,
                dest,
                startTime,
                endTime,
                cost,
                vehicles != null ? vehicles.stream().map(Vehicle::toDto).toList() : null,
                type,
                maxSeats
        );
    }
}
