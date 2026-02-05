import http from "k6/http";
import { check } from "k6";

// 1. 계정 목록 로딩
const users = JSON.parse(open("./users.json"));

// 2. 테스트 옵션
export let options = {
  vus: 1000,
  duration: "20s",
};

// 3. VU 실행 로직
export default function () {
  // 각 VU는 고유 계정 사용
  const user = users[__VU - 1];

  const userId = user.userId;
  const fakeIp = `10.0.${Math.floor(__VU / 256)}.${__VU % 256}`;
  const res = http.post(
    `http://localhost:8080/coupons/2/issue/${userId}`,
    null,
    { headers: { "X-Forwarded-For": fakeIp } },
  );
  check(res, {
    "coupon result ok": (r) => r.status === 200 || r.status === 409,
  });
}
