# short-url

로그인 없이 긴 URL을 `https://도메인/42`처럼 **숫자 주소**로 줄여 주는 URL 단축 서비스.

- 같은 URL을 다시 줄이면 기존 주소를 그대로 돌려줍니다(동시 요청에도 하나만 생성).
- 만든 링크는 영구 보존되며, 링크 생성은 IP당 분당 10회로 제한됩니다.
- 존재하지 않는 주소는 "링크를 찾을 수 없습니다" 페이지(404)를 보여 줍니다.

## 기술 스택

| 영역 | 사용 기술 |
|---|---|
| 백엔드 (`backend/`) | Spring Boot 4, Kotlin, Java 21, Spring Data JPA, Flyway, PostgreSQL, Bucket4j |
| 프론트엔드 (`frontend/`) | Vite, React, TypeScript |
| 테스트 | JUnit + Testcontainers(실제 PostgreSQL), Vitest + React Testing Library |
| 배포 | Docker 단일 이미지 + Caddy(자동 HTTPS) + PostgreSQL, `docker compose` |

## API

| 요청 | 응답 |
|---|---|
| `POST /api/links` `{ "targetUrl": "https://..." }` | 새로 만들면 `201`, 이미 있으면 `200` — `{ code, shortUrl, targetUrl }` |
| 잘못된 URL / 요청 제한 초과 | `400` / `429` — `application/problem+json`(RFC 9457) |
| `GET /{code}` | `302` 리다이렉트, 없는 Code는 `404` |

## 실행 방법

Docker가 실행 중이어야 합니다.

### 운영과 같은 구성으로 실행 (가장 간단)

프로젝트 루트에 `.env` 파일을 만듭니다.

```
DOMAIN=localhost
PUBLIC_BASE_URL=https://localhost
POSTGRES_PASSWORD=any-local-password
```

```bash
docker compose up -d --build
```

브라우저에서 **https://localhost** 를 엽니다(로컬 인증서라 처음 한 번 보안 경고가 뜹니다). 종료는 `docker compose down`.

### 개발 모드

```bash
docker compose -f compose.dev.yaml up -d   # 개발용 PostgreSQL
cd backend && ./gradlew bootRun            # 백엔드 :8080 (JDK 21 필요)
cd frontend && npm install && npm run dev  # 프론트엔드 :5173
```

**http://localhost:5173** 을 엽니다. Vite가 `/api`와 숫자 경로를 백엔드로 전달합니다.

### 테스트

```bash
cd backend && ./gradlew test   # Docker 필요(Testcontainers)
cd frontend && npm test
```

## 문서

- [GLOSSARY.md](GLOSSARY.md) — 도메인 용어(Short Link, Code, Target URL)
- [docs/adr/](docs/adr/) — 설계 결정(순번 숫자 Code, 계정 없음, 302 리다이렉트)
- [docs/deploy.md](docs/deploy.md) — 서버 배포 절차
- [.scratch/url-shortener-v1/](.scratch/url-shortener-v1/) — 스펙과 구현 티켓
