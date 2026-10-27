# 01: 워킹 스켈레톤 — Short Link 만들고 따라가기

**What to build:** 방문자가 메인 페이지에 Target URL을 넣고 제출하면 숫자 Code가 들어간 Short Link가 화면에 나타나고, 그 주소를 열면 Target URL로 이동한다. 이 과정에서 `backend/`(Spring Boot 4 + Kotlin, Java 21, Gradle Kotlin DSL)와 `frontend/`(Vite + React + TypeScript) 뼈대, 로컬 Postgres(Docker Compose), 첫 Flyway 마이그레이션, 두 테스트 seam의 첫 테스트를 함께 세운다. 검증은 이 티켓에서는 `http`/`https` scheme 확인만 한다. 전체 규칙은 02에서 다룬다. 스펙: `.scratch/url-shortener-v1/spec.md`.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [x] 로컬 개발용 Docker Compose로 PostgreSQL을 띄울 수 있다.
- [x] Flyway 마이그레이션이 Short Link 테이블을 만든다. Code는 1부터 시작하는 identity 순번이고, 정규화된 Target URL과 생성 시각을 저장한다.
- [x] `POST /api/links`에 `{ "targetUrl": "https://example.com" }`을 보내면 `201`과 `{ code, shortUrl, targetUrl }`을 돌려준다. `shortUrl`은 설정된 공개 base URL에 Code를 붙인 값이다.
- [x] `http`/`https`가 아닌 Target URL은 `400`으로 거부한다.
- [x] `GET /{code}`는 숫자 경로만 받고, 존재하는 Code면 `302`와 `Location: <Target URL>`로 응답한다.
- [x] 메인 페이지에서 Target URL을 제출하면 결과 Short Link와 Target URL이 화면에 표시된다.
- [x] Vite dev server가 `/api/**`와 숫자 경로를 백엔드로 프록시해서, 로컬에서 프론트와 백엔드를 따로 띄워 개발할 수 있다.
- [x] 백엔드 HTTP 경계 테스트(앱 전체 + Testcontainers Postgres)가 생성 201과 리다이렉트 302를 검증하고 통과한다.
- [x] 프론트 페이지 경계 테스트(Vitest + React Testing Library, fetch 대체)가 제출 후 결과 표시를 검증하고 통과한다.
