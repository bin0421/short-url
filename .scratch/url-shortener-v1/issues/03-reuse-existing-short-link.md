# 03: 기존 Short Link 재사용

**What to build:** 이미 Short Link가 있는 Target URL을 다시 제출하면 새 Code 대신 기존 Short Link를 받는다. scheme이나 host의 대소문자만 다른 주소도 같은 Target URL로 본다. 여러 사람이 같은 Target URL을 동시에 제출해도 Short Link는 하나만 생긴다(ADR 0001).

**Blocked by:** 02 (Target URL 규칙과 오류 메시지)

**Status:** ready-for-agent

- [x] 정규화된 Target URL에 unique 제약을 거는 Flyway 마이그레이션이 있다.
- [x] 기존 Short Link가 있는 Target URL을 제출하면 `200 OK`와 기존 `code`, `shortUrl`을 돌려준다. 새로 만들 때는 계속 `201 Created`이다.
- [x] `https://Example.COM/a`를 제출한 뒤 `https://example.com/a`를 제출하면 같은 Code를 받는다.
- [x] 경로, 쿼리, 끝 슬래시만 다른 주소는 서로 다른 Code를 받는다.
- [x] 동시 삽입으로 unique 제약 위반이 나면 기존 행을 다시 조회해 `200`으로 돌려준다.
- [x] HTTP 경계 테스트가 같은 Target URL을 동시에 여러 번 제출해도 Short Link가 하나만 생기고 모든 응답이 같은 Code임을 검증한다.
- [x] 메인 페이지는 `200`과 `201` 응답을 똑같이 결과로 표시한다.
