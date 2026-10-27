# 운영 배포

운영 환경은 Docker가 설치된 Linux VM 한 대에서 `compose.yaml` 하나로 띄운다. 서비스는 세 개다.

- `caddy`: 80/443을 받아 HTTPS를 처리하고 `app:8080`으로 프록시한다. Let's Encrypt 인증서를 자동으로 발급·갱신한다.
- `app`: 루트 `Dockerfile`로 빌드한 단일 이미지(Spring Boot가 프론트까지 서빙).
- `postgres`: PostgreSQL 17. 데이터는 named volume `postgres-data`에 남는다.

외부에 포트를 여는 것은 Caddy(80, 443, 443/udp)뿐이다. 앱은 요청 제한에 `X-Forwarded-For`를 믿으므로 app의 8080과 Postgres의 5432는 절대 외부에 publish하지 않는다.

## 최초 서버 준비

1. Linux VM을 만들고 Docker Engine과 Compose 플러그인을 설치한다(`docker compose version`으로 확인).
2. 도메인의 DNS A 레코드(IPv6를 쓰면 AAAA도)를 VM의 공인 IP로 지정한다.
3. 방화벽/보안 그룹에서 TCP 80, TCP 443, UDP 443(HTTP/3)을 연다. 80은 인증서 발급(HTTP-01)과 HTTPS 리다이렉트에 필요하다.
4. 저장소를 받는다.

   ```sh
   git clone <저장소 URL> short-url
   cd short-url
   ```

5. 환경변수 파일을 만들고 값을 채운다. `.env`는 `.gitignore`에 있어 커밋되지 않는다.

   ```sh
   cp .env.example .env
   ```

   | 변수 | 설명 |
   | --- | --- |
   | `DOMAIN` | 서비스 도메인(예: `short.example.com`). Caddy의 사이트 주소가 된다. |
   | `PUBLIC_BASE_URL` | 응답의 `shortUrl`을 만들 base URL(예: `https://short.example.com`). |
   | `POSTGRES_PASSWORD` | DB 비밀번호. `openssl rand -hex 32` 같은 긴 임의 값을 쓴다. |

6. 처음 띄운다.

   ```sh
   docker compose up -d --build
   ```

   DNS가 맞게 잡혀 있으면 Caddy가 몇 초 안에 인증서를 받는다. `https://<DOMAIN>`으로 접속해 확인한다.

## 배포(수동)

```sh
cd short-url
git pull
docker compose up -d --build
```

컨테이너는 다시 만들어지지만 `postgres-data`(Short Link)와 `caddy-data`(인증서) volume은 그대로 남는다. `docker compose down -v`는 volume까지 지우므로 운영에서 쓰지 않는다.

## 상태와 로그 확인

```sh
docker compose ps                  # 서비스 상태(postgres는 healthy여야 한다)
docker compose logs -f app         # 앱 로그
docker compose logs -f caddy       # 프록시·인증서 발급 로그
docker compose logs --tail=100     # 전체 최근 로그
```
