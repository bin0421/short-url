# short-url

긴 URL을 짧은 숫자 주소로 바꿔 주는 공개 서비스. 로그인 없이 누구나 Short Link를 만들고 따라갈 수 있다.

## Language

**Short Link**:
Target URL 하나와 Code 하나를 묶은 영구 기록. 한 번 만들어지면 바뀌거나 사라지지 않는다.
_Avoid_: 단축 URL, link, mapping, alias

**Code**:
Short Link를 가리키는 10진수 숫자로, 짧은 주소의 경로 부분(`/123`의 `123`)이다. Short Link가 만들어진 순서대로 매겨진다.
_Avoid_: slug, short code, key, id

**Target URL**:
사용자가 줄이려고 제출한 원래 주소. Short Link를 따라가면 이 주소로 이동한다. scheme과 host의 대소문자만 다른 두 주소는 같은 Target URL이다.
_Avoid_: 원본 URL, long URL, original URL, destination
