package edu.pg.booking.repository;

import edu.pg.booking.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByEndTimeAfter(Date currentTime);

}
