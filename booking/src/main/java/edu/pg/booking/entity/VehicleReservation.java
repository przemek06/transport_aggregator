package edu.pg.booking.entity;

import edu.pg.booking.dto.VehicleReservationDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String vehicleId;
    private Date startTime;
    private Date endTime;
    @ManyToOne
    @JoinColumn(name="reservation_id", nullable = false)
    private Reservation reservation;

    public VehicleReservationDto toDto() {
        return new VehicleReservationDto(
                id, vehicleId, startTime, endTime
        );
    }
}