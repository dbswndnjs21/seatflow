package com.seatflow.station.service;

import com.seatflow.station.domain.Station;
import com.seatflow.station.repository.StationRepository;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StationService {

    private static final List<String> DEFAULT_STATIONS = List.of(
        "서울", "용산", "광명", "영등포", "수원", "평택", "천안아산", "천안", "오송", "조치원",
        "대전", "서대전", "김천구미", "구미", "동대구", "대구", "경주", "울산(통도사)", "포항", "경산",
        "밀양", "부산", "구포", "창원중앙", "평창", "진부(오대산)", "강릉", "익산", "전주", "광주송정",
        "목포", "순천", "청량리", "여수EXPO", "동해", "정동진", "안동", "서원주", "원주", "마산",
        "행신", "나주", "정읍", "남원"
    );

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public List<Station> findStations(String query) {
        if (!StringUtils.hasText(query)) {
            return stationRepository.findAllByOrderByDisplayOrderAscNameAsc();
        }
        return stationRepository.findByNameContainingIgnoreCaseOrderByDisplayOrderAscNameAsc(query.trim());
    }

    @Transactional
    public void seedStationsIfEmpty() {
        if (stationRepository.count() > 0) {
            return;
        }

        List<Station> stations = IntStream.range(0, DEFAULT_STATIONS.size())
            .mapToObj(index -> new Station(DEFAULT_STATIONS.get(index), index + 1))
            .toList();

        stationRepository.saveAll(stations);
    }
}
