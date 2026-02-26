# seatFlow

Spring Boot 기반 KTX 예매 백엔드/웹 프로젝트입니다.  
목표는 단순 CRUD가 아니라, 좌석 선점 구간의 동시성 제어와 트래픽 상황 대응을 설계/검증하는 것입니다.

## 1. 프로젝트 목표
- 로그인/회원가입부터 열차 조회, 좌석 조회, 좌석 선점(HOLD)까지 실제 예매 흐름 구현
- 좌석 중복 선점 방지를 위한 다중 방어 계층 구현
- 대량 동시 요청에서 실패/성공 패턴과 병목 지점 관찰

## 2. 기술 스택
- Java 17, Spring Boot 4.0.3
- Spring Security (폼 로그인)
- Spring Data JPA + JdbcTemplate
- MariaDB
- Redis + Redisson (분산락, Watchdog)
- k6 (동시성/부하 테스트)
- Docker Compose (Redis 실행)

## 3. 현재 구현 범위
### 사용자/인증
- 회원가입 API: `POST /api/auth/signup`
- 로그인 페이지/회원가입 페이지 커스텀 UI
- 로그인 세션 기반 사용자 정보 조회: `GET /api/auth/me`

### 메인/검색
- 출발역/도착역 모달 검색 UI
- 역 검색 API: `GET /api/stations`, `GET /api/stations?q=...`
- 열차 조회 API: `GET /api/trains/search?from=&to=&departureDateTime=...`

### 좌석
- 좌석 조회 API: `GET /api/train-runs/{runId}/seats`
- 좌석 선점 API: `POST /api/seats/{seatInventoryId}/hold`
- 선점 만료 스케줄러: 만료 시 `HELD -> AVAILABLE` 자동 복구

### 데이터 초기화
- 역/열차/좌석 더미 데이터 초기화 토글 제공
- 초기화 실행 여부를 `application.properties`에서 제어

## 4. 핵심 설계 (동시성)
좌석 선점 시 아래 순서로 처리합니다.

1. Redis 분산락 키 획득 시도 (`seat:hold:{seatInventoryId}`)
2. 락 획득 실패 시 즉시 `409 CONFLICT` 반환
3. 락 획득 성공 시 DB 트랜잭션 진입
4. `SELECT ... FOR UPDATE`로 좌석 row 잠금
5. 상태 확인 후 `AVAILABLE/만료 HELD`만 `HELD`로 전이
6. 커밋 후 `finally`에서 Redis unlock

보강 포인트:
- Redisson Watchdog 사용: 트랜잭션 처리 중 락 TTL 자동 연장
- 비정상 종료 시 watchdog 연장 중단 + TTL 만료로 락 자동 해제
- 최종 상태의 source of truth는 DB (`seat_inventory`)

## 5. 상태 모델
`seat_inventory.availability_status`
- `AVAILABLE`: 선택 가능
- `HELD`: 임시 선점(결제 대기)
- `RESERVED`: 예약 확정

관련 컬럼:
- `hold_token`: 선점 토큰
- `hold_expires_at`: 선점 만료 시각

## 6. 도메인 구조 요약
- `app_user`: 사용자
- `station`: 역 마스터
- `train`, `train_run`, `train_car`, `seat`: 열차/운행/객차/좌석
- `seat_inventory`: 운행편별 좌석 상태
- `reservation`, `reservation_seat`, `reservation_passenger`: 예약 도메인
- `payment`, `payment_attempt`, `payment_idempotency`, `outbox_event`: 결제/정합성 확장 도메인

## 7. 부하 테스트 (k6)
테스트 파일: `k6/seat-hold.js`

예시:
```bash
k6 run k6/seat-hold.js -e BASE_URL=http://localhost:8080 -e SEAT_ID=323 -e COOKIE="JSESSIONID=..." -e VUS=30 -e DURATION=5s
```

관찰 포인트:
- 같은 좌석 ID에 동시 요청 시 보통 1건 성공, 나머지 `409` 충돌
- 분산락 + DB 락 조합이 중복 선점을 차단하는지 확인 가능

## 8. 트러블슈팅
### 8.1 DB 락 대기열 폭증 문제
문제:
- 같은 좌석에 동시 요청이 몰릴 때 모든 요청이 DB까지 진입해 lock wait이 누적되고 응답시간이 급격히 증가

개선:
- Redis 분산락을 선점 API 앞단에 두어 중복 요청을 즉시 차단(빠른 409 반환)

결과:
- DB 락 경합 요청 수가 감소하고, tail latency(p95/p99)가 안정화됨

### 8.2 고정 TTL 락의 조기 만료 리스크
문제:
- 비즈니스 처리 시간이 길어질 때 고정 leaseTime 기반 락이 먼저 만료될 가능성 존재

개선:
- Redisson watchdog 기반으로 전환해 처리 중 락 TTL을 자동 연장하도록 변경

결과:
- 처리 중 락 만료로 인한 중간 해제 리스크를 줄이고, 정상 흐름에서는 `finally`에서 unlock 수행

### 8.3 좀비 HOLD(영구 점유) 문제
문제:
- 결제 없이 이탈한 요청이 `HELD` 상태를 남기면 좌석이 계속 판매 불가 상태로 고착

개선:
- `hold_expires_at`을 두고 스케줄러에서 만료 건을 주기적으로 `AVAILABLE`로 복구

결과:
- 운영자 개입 없이 자동 회수되며 좌석 회전율 유지

## 9. 설정
주요 설정 파일: `src/main/resources/application.properties`

핵심 프로퍼티:
- `app.init.station.enabled`
- `app.init.demo.enabled`
- `app.hold.lock-wait-ms`
- `app.hold.duration-minutes`
- `app.hold.expire.enabled`
- `app.hold.expire.fixed-delay-ms`
- `spring.data.redis.host`, `spring.data.redis.port`

Redis 실행:
```bash
docker compose up -d
```

## 10. 다음 단계
- 결제 성공 시 `HELD -> RESERVED` 확정 API
- 예약 취소/내역 조회(마이페이지) API
- 구간 예매(부분 구간 점유) 모델
- Redis 장애 시 fallback 정책/재시도 정책 고도화
- 좌석 선점 성능 테스트를 전체 사용자 시나리오 테스트로 확장
