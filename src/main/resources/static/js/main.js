document.addEventListener('DOMContentLoaded', () => {
    const departure = document.getElementById('departure-trigger');
    const arrival = document.getElementById('arrival-trigger');
    const datetime = document.getElementById('departure-datetime');
    const passengers = document.getElementById('passengers');
    const swapBtn = document.getElementById('swap-btn');
    const searchBtn = document.getElementById('search-btn');
    const stationModal = document.getElementById('station-modal');
    const stationModalTitle = document.getElementById('station-modal-title');
    const stationSearch = document.getElementById('station-search');
    const stationList = document.getElementById('station-list');
    const stationModalClose = document.getElementById('station-modal-close');
    const stationBackdrop = stationModal.querySelector('[data-close="true"]');
    const trainResultModal = document.getElementById('train-result-modal');
    const trainResultBackdrop = trainResultModal.querySelector('[data-train-close="true"]');
    const trainResultClose = document.getElementById('train-result-close');
    const trainResultList = document.getElementById('train-result-list');
    const authLinks = document.getElementById('auth-links');
    const userGreeting = document.getElementById('user-greeting');
    const userName = document.getElementById('user-name');

    let activeTarget = 'departure';
    let searchRequestSeq = 0;

    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const defaultValue = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
    datetime.value = defaultValue;

    const renderStationList = (stationNames) => {
        if (!stationNames.length) {
            stationList.innerHTML = '<div class="station-empty">검색 결과가 없습니다.</div>';
            return;
        }

        stationList.innerHTML = stationNames.map((station) =>
            `<button class="station-item-btn" type="button" data-station="${station}">${station}</button>`
        ).join('');
    };

    const fetchStations = async (query = '') => {
        const url = query ? `/api/stations?q=${encodeURIComponent(query)}` : '/api/stations';
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`stations api failed: ${response.status}`);
        }
        const data = await response.json();
        return data.map((item) => item.name);
    };

    const loadMe = async () => {
        try {
            const response = await fetch('/api/auth/me');
            if (!response.ok) {
                return;
            }
            const me = await response.json();
            if (authLinks) {
                authLinks.hidden = true;
            }
            if (userGreeting && userName) {
                userName.textContent = me.name || me.loginId;
                userGreeting.hidden = false;
            }
        } catch (error) {
            // ignore
        }
    };

    const closeTrainResultModal = () => {
        trainResultModal.hidden = true;
    };

    const openTrainResultModal = () => {
        trainResultModal.hidden = false;
    };

    const formatDateTime = (value) => {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        const y = date.getFullYear();
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const d = String(date.getDate()).padStart(2, '0');
        const hh = String(date.getHours()).padStart(2, '0');
        const mm = String(date.getMinutes()).padStart(2, '0');
        return `${y}-${m}-${d} ${hh}:${mm}`;
    };

    const renderTrainResults = (trains) => {
        if (!trains.length) {
            trainResultList.innerHTML = '<div class="station-empty">조회된 열차가 없습니다.</div>';
            return;
        }

        trainResultList.innerHTML = trains.map((train) => {
            const durationHours = Math.floor(train.durationMinutes / 60);
            const durationMins = train.durationMinutes % 60;
            const durationText = `${durationHours}시간 ${durationMins}분`;
            const fare = Number(train.baseFare).toLocaleString('ko-KR');
            const departureDisplay = formatDateTime(train.departureTime);
            const seatSelectUrl = `/seat-selection.html?runId=${encodeURIComponent(train.runId)}&from=${encodeURIComponent(train.departureStation)}&to=${encodeURIComponent(train.arrivalStation)}&departure=${encodeURIComponent(departureDisplay)}`;
            return `
                <div class="train-result-item">
                    <div class="train-result-head">
                        <div>
                            <div class="train-result-no">${train.trainNo}</div>
                            <div class="train-result-type">${train.trainType}</div>
                        </div>
                        <div class="train-result-time">${formatDateTime(train.departureTime)} 출발</div>
                    </div>
                    <div class="train-result-meta">
                        <span>${train.departureStation} → ${train.arrivalStation}</span>
                        <span>소요 ${durationText}</span>
                        <span>잔여 ${train.availableSeats}석</span>
                        <span>요금 ${fare}원</span>
                        <a href="${seatSelectUrl}" class="seat-select-link">좌석선택</a>
                    </div>
                </div>
            `;
        }).join('');
    };

    const fetchTrainResults = async () => {
        const from = departure.textContent.trim();
        const to = arrival.textContent.trim();
        const departureDateTime = datetime.value;

        if (!from || !to || !departureDateTime) {
            alert('출발역, 도착역, 출발일을 확인해주세요.');
            return;
        }

        const query = new URLSearchParams({
            from,
            to,
            departureDateTime
        });

        const response = await fetch(`/api/trains/search?${query.toString()}`);
        if (!response.ok) {
            throw new Error(`train search failed: ${response.status}`);
        }
        return response.json();
    };

    const openStationModal = async (target) => {
        activeTarget = target;
        stationModalTitle.textContent = '기차역 조회';
        stationSearch.value = '';
        stationModal.hidden = false;
        stationSearch.focus();

        try {
            const stationNames = await fetchStations('');
            renderStationList(stationNames);
        } catch (error) {
            stationList.innerHTML = '<div class="station-empty">역 목록을 불러오지 못했습니다.</div>';
        }
    };

    const closeStationModal = () => {
        stationModal.hidden = true;
    };

    departure.addEventListener('click', () => openStationModal('departure'));
    arrival.addEventListener('click', () => openStationModal('arrival'));

    stationSearch.addEventListener('input', async (event) => {
        const query = event.target.value.trim();
        const currentSeq = ++searchRequestSeq;

        try {
            const stationNames = await fetchStations(query);
            if (currentSeq !== searchRequestSeq) {
                return;
            }
            renderStationList(stationNames);
        } catch (error) {
            if (currentSeq !== searchRequestSeq) {
                return;
            }
            stationList.innerHTML = '<div class="station-empty">역 목록을 불러오지 못했습니다.</div>';
        }
    });

    stationList.addEventListener('click', (event) => {
        const target = event.target.closest('.station-item-btn');
        if (!target) return;

        const selected = target.dataset.station;
        if (activeTarget === 'departure') {
            departure.textContent = selected;
        } else {
            arrival.textContent = selected;
        }
        closeStationModal();
    });

    stationModalClose.addEventListener('click', closeStationModal);
    stationBackdrop.addEventListener('click', closeStationModal);
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !stationModal.hidden) {
            closeStationModal();
        }
    });

    swapBtn.addEventListener('click', () => {
        const temp = departure.textContent;
        departure.textContent = arrival.textContent;
        arrival.textContent = temp;
    });

    trainResultClose.addEventListener('click', closeTrainResultModal);
    trainResultBackdrop.addEventListener('click', closeTrainResultModal);

    searchBtn.addEventListener('click', async () => {
        try {
            const trains = await fetchTrainResults();
            if (!Array.isArray(trains)) {
                return;
            }
            renderTrainResults(trains);
            openTrainResultModal();
        } catch (error) {
            alert('열차 조회 중 오류가 발생했습니다.');
        }
    });

    loadMe();
});
