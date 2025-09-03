- Harvest가 보낸 카프카 메시지를 수신해서 정제 작업을 한다.


- 뉴스
  - MinIO에서 html을 조회
  - 중복제거
      - 기사 제목과 본문처럼 핵심적인 내용만 추출
      - 해시함수를 태워서 해시값을 redis에 저장. 새로운 페이지 수집 시 같은 방법으로 해시를 비교.
      - 이미 존재하는 경우 무시 혹은 원본도 삭제
- 위키
  - 중복 제거
    - 문서를 파싱해서 id를 확인
    - id가 없는 경우: 신규 페이지기 때문에 생성
    - id가 있는 경우: revisionId를 비교
        - revisionId가 없는 경우: 새로운 수정본이다. 덮어쓰기
        - revisionId가 있는 경우: 이미 있는 데이터. 무시하기
  - 원본 url을 생성하는 방법
    - siteinfo.base 태그 안에 baseUrl이 있다. (예: https://ko.wikipedia.org/wiki) 
    - page.title 태그에 있는 제목을 baseUrl 뒤에 붙이면 된다. (예: `{baseUrl}/{title}`)
- X API
