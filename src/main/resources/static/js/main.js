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

    searchBtn.addEventListener('click', () => {
        const message = `열차 조회\n출발: ${departure.textContent}\n도착: ${arrival.textContent}\n출발일: ${datetime.value.replace('T', ' ')}\n인원: ${passengers.options[passengers.selectedIndex].text}`;
        alert(message);
    });
});
