- 파이썬으로 작업 

- 뉴스
  - 수집 시 중복 제거
    - url을 정규화한다.
      - 프로토콜 통일, www 제거, 파라미터 제거, 정렬
    - Canonical 태그 확인
    - url을 redis에 저장해두고 중복된 url이라면 무시한다.
  - url로 html을 요청한다.
  - html을 MinIO에 저장한다.
- 위키
  - 압축파일(.bz2) 자체를 MinIO에 넣는다.
- X api
  - 내가 받아온 다음부터 API를 호출해서 json 응답을 받는다.
    - 이미 저장한 데이터를 다시 호출하면 안되기 때문에 어디까지 받아왔는지 기억해둔다. 이후 또 호출 시에는 그 다음부터 요청한다.
  - 응답을 json 파일로 만들어서 MiniO에 저장한다.

- 데이터 저장
  - 원본 데이터는 MiniO에 저장한다.


- 카프카 메시지
    ```
    {
      "source": "naver_news",
      "storage_key": "news/2025/09/03/article_12345.html",
      "original_url": "https://n.news.naver.com/article/123/0004567890",
      "collected_at": "2025-09-03T08:20:00Z"
    }
    ```