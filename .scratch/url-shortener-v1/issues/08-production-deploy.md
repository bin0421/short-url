# 08: 운영 배포

**What to build:** 운영자가 Docker가 설치된 Linux VM 한 대에서 명령 하나로 Caddy, 앱, PostgreSQL을 띄우고, 도메인으로 HTTPS 접속할 수 있다. 인증서는 자동으로 발급·갱신되고, 다시 배포해도 Short Link 데이터는 유지된다.

**Blocked by:** 05 (단일 이미지 — 백엔드가 프론트 서빙)

**Status:** ready-for-agent

- [x] 운영용 compose 정의가 Caddy, app, Postgres 세 서비스를 띄운다.
- [ ] Caddy가 설정된 도메인으로 Let's Encrypt 인증서를 자동 발급하고, app으로 프록시하면서 `X-Forwarded-For`를 넘긴다.
- [x] Postgres 데이터가 named volume에 저장되어, 컨테이너를 다시 만들어도 Short Link가 남는다.
- [x] 도메인, 공개 base URL, DB 비밀번호는 환경변수(예시 파일 포함)로 넣고, 실제 비밀값은 저장소에 커밋하지 않는다.
- [x] 수동 배포 절차(`git pull` → `docker compose up -d --build`)와 최초 서버 준비 단계가 문서로 남아 있다.
- [x] 로컬에서 운영용 compose를 띄워 Short Link 생성과 리다이렉트가 동작함을 확인한다.
