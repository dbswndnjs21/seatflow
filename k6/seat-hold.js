import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

export const options = {
  vus: Number(__ENV.VUS || 30),
  duration: __ENV.DURATION || '5s',
};

const holdSuccess = new Counter('hold_success_200');
const holdConflict = new Counter('hold_conflict_409');
const holdOther = new Counter('hold_other_status');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SEAT_ID = __ENV.SEAT_ID || '1';
const COOKIE = __ENV.COOKIE || ''; // 예: JSESSIONID=xxxx

if (!COOKIE) {
  throw new Error('COOKIE 환경변수(JSESSIONID=...)가 필요합니다.');
}

export default function () {
  const res = http.post(`${BASE_URL}/api/seats/${SEAT_ID}/hold`, null, {
    headers: { Cookie: COOKIE },
  });

  if (res.status === 200) {
    holdSuccess.add(1);
  } else if (res.status === 409) {
    holdConflict.add(1);
  } else {
    holdOther.add(1);
  }

  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}
