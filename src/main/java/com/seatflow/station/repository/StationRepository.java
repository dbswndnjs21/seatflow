package com.seatflow.station.repository;

import com.seatflow.station.domain.Station;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findAllByOrderByDisplayOrderAscNameAsc();

    List<Station> findByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(String keyword);
}
