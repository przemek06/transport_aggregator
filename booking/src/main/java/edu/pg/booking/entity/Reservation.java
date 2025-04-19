package edu.pg.booking.entity;

import edu.pg.booking.dto.ReservationDto;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private Date startTime;
    private Date endTime;
    private Instant reservationTime;

    public ReservationDto toDto() {
        return new ReservationDto(
                id, username, startTime, endTime, reservationTime
        );
    }
}