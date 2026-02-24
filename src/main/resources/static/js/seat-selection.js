document.addEventListener('DOMContentLoaded', async () => {
    const seatContainer = document.getElementById('seat-container');
    const summary = document.getElementById('summary');

    const params = new URLSearchParams(location.search);
    const runId = params.get('runId');
    const from = params.get('from');
    const to = params.get('to');
    const departure = params.get('departure');

    summary.textContent = `열차편 #${runId || '-'} | ${from || '-'} → ${to || '-'} | 출발 ${departure || '-'}`;

    if (!runId) {
        seatContainer.innerHTML = '<div class="empty">잘못된 접근입니다. 조회 화면에서 다시 선택해주세요.</div>';
        return;
    }

    await loadSeats();

    function renderSeats(seats) {
        if (!Array.isArray(seats) || seats.length === 0) {
            seatContainer.innerHTML = '<div class="empty">조회된 좌석이 없습니다.</div>';
            return;
        }

        const byCar = seats.reduce((acc, seat) => {
            const key = `${seat.carNo}`;
            if (!acc[key]) {
                acc[key] = { classType: seat.classType, seats: [] };
            }
            acc[key].seats.push(seat);
            return acc;
        }, {});

        const carNos = Object.keys(byCar).map(Number).sort((a, b) => a - b);

        seatContainer.innerHTML = carNos.map((carNo) => {
            const group = byCar[carNo];
            const seatButtons = group.seats.map((seat) => {
                const status = normalizeStatus(seat.status);
                const disabled = status !== 'available' ? 'disabled' : '';
                return `<button class="seat-btn ${status}" ${disabled} data-seat-inventory-id="${seat.seatInventoryId}">${seat.seatNo}</button>`;
            }).join('');

            return `
                <div class="car">
                    <div class="car-head">
                        <span class="car-title">${carNo}호차</span>
                        <span class="car-class">${group.classType}</span>
                    </div>
                    <div class="seat-grid">${seatButtons}</div>
                </div>
            `;
        }).join('');
    }

    function normalizeStatus(status) {
        const value = (status || '').toUpperCase();
        if (value === 'AVAILABLE') return 'available';
        if (value === 'HELD') return 'held';
        if (value === 'RESERVED') return 'reserved';
        return 'reserved';
    }

    async function loadSeats() {
        try {
            const response = await fetch(`/api/train-runs/${runId}/seats`);
            if (!response.ok) {
                throw new Error(`seat api failed: ${response.status}`);
            }
            const seats = await response.json();
            renderSeats(seats);
        } catch (error) {
            seatContainer.innerHTML = '<div class="empty">좌석 정보를 불러오지 못했습니다.</div>';
        }
    }

    seatContainer.addEventListener('click', async (event) => {
        const target = event.target.closest('.seat-btn.available');
        if (!target) {
            return;
        }

        const seatInventoryId = target.dataset.seatInventoryId;
        if (!seatInventoryId) {
            return;
        }

        target.disabled = true;

        try {
            const response = await fetch(`/api/seats/${seatInventoryId}/hold`, { method: 'POST' });
            const payload = await response.json().catch(() => ({}));

            if (!response.ok) {
                alert(payload.message || payload.detail || '좌석 선점에 실패했습니다.');
                await loadSeats();
                return;
            }

            alert(`좌석 선점 완료 (만료: ${payload.holdExpiresAt})`);
            await loadSeats();
        } catch (error) {
            alert('좌석 선점 중 오류가 발생했습니다.');
            await loadSeats();
        }
    });
});
