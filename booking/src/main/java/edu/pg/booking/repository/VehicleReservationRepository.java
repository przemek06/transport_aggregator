package edu.pg.booking.repository;

import edu.pg.booking.entity.VehicleReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface VehicleReservationRepository extends JpaRepository<VehicleReservation, Long> {

    List<VehicleReservation> findByVehicleIdAndEndTimeGreaterThanEqualAndStartTimeLessThanEqual(
            String vehicleId,
            Date segmentStartTime,
            Date segmentEndTime
    );
}