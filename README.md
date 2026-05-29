# Community Project

다중 게시판 기반 커뮤니티 백엔드 프로젝트입니다.

단순 CRUD 구현에 그치지 않고, 실제 서비스 운영 환경에서 발생할 수 있는 인증/인가, 데이터 정합성, 캐싱, 검색 성능, 실시간 알림 등의 문제를 직접 고민하고 설계하는 것을 목표로 개발하고 있습니다.

---

## 프로젝트 배경

기존 프로젝트에서는 CRUD 기능 구현 자체에 집중했다면, 이번 프로젝트에서는 "서비스를 어떻게 안정적으로 운영할 것인가"에 초점을 맞추었습니다.

특히 커뮤니티 서비스에서 자주 사용되는 기능들을 직접 구현하며 다음과 같은 부분을 경험하는 것을 목표로 하고 있습니다.

* 인증/인가 및 권한 관리
* 데이터 정합성과 유지보수성 고려
* Redis 기반 캐싱 구조 학습
* QueryDSL 기반 동적 조회 및 페이징
* 검색 성능 개선 및 부하 테스트 경험
* 실시간 알림 및 운영 환경 고려

또한 디시인사이드의 갤러리 구조를 참고하여 여러 게시판을 생성 및 운영할 수 있는 범용 커뮤니티 구조로 설계하였습니다.

---

## 기술 스택

### Backend

* Java 17
* Spring Boot 3
* Spring Security
* JPA / QueryDSL
* JWT

### Database & Cache

* MySQL
* Redis

### Infra

* Docker
* AWS EC2
* GitHub Actions (예정)

### Frontend

* Vanilla JS
* Bootstrap 5

---

## 진행 상황

> 현재 개발 진행 중인 프로젝트입니다. (2026.05 ~)

| 내용                          | 상태    |
| --------------------------- | ----- |
| Git 초기 설정 및 브랜치 전략          | ✅ 완료  |
| 패키지 구조 설계                   | ✅ 완료  |
| BaseEntity / JPA Auditing   | ✅ 완료  |
| DB 스키마 설계                   | ✅ 완료  |
| 핵심 엔티티 작성                   | ✅ 완료  |
| 공통 예외 처리                    | ✅ 완료  |
| Spring Security + JWT 인증/인가 | ✅ 완료  |
| 회원가입 / 로그인 API              | ✅ 완료  |
| 게시판 기능                      | ✅ 완료  |
| QueryDSL 설정                 | ✅ 완료  |
| 게시글 CRUD                    | ✅ 완료  |
| 댓글 / 대댓글 기능                 | ✅ 완료  |
| 추천 / 좋아요 기능                 | 진행 예정 |
| Redis 조회수 캐싱                | 진행 예정 |
| 검색 기능 개선 (LIKE → FULLTEXT)  | 진행 예정 |
| SSE 실시간 알림                  | 진행 예정 |
| Swagger / OpenAPI           | 진행 예정 |
| S3 이미지 업로드                  | 진행 예정 |
| OAuth2 로그인                  | 진행 예정 |
| Docker + AWS EC2 배포 + CI/CD | 진행 예정 |


---

## 구현 기능

### 완료 기능

* 회원가입 / 로그인 (JWT)
* AccessToken / RefreshToken 관리
* Redis 기반 RefreshToken 저장
* 다중 게시판 CRUD
* 게시판 관리자 권한 관리
* 게시글 CRUD
* 댓글 / 대댓글 기능
* Soft Delete 기반 삭제 처리
* QueryDSL 기반 페이징 조회
* 공통 예외 처리 구조
* 공통 응답 DTO 구성

---

## 테스트

* JUnit5 + BDDMockito + AssertJ 기반 테스트 코드 작성
* Service 레이어 단위 테스트 진행
* 기능 구현 단계부터 테스트 코드 병행 작성

---

## Git 전략

* main / dev 브랜치 분리
* feature 브랜치 단위 개발 진행
* PR 및 Merge 기반 작업 관리

개인 프로젝트이지만 협업 환경을 고려한 개발 습관을 유지하려 노력하고 있습니다.

---

## ERD

<img src="docs/images/ERD_table.png.png" width="800"/>

> 상세 ERD는 이미지를 확대하여 확인 가능합니다.

- 댓글/대댓글은 self-reference(parent_id) 구조로 설계
- 게시판 관리자 권한은 BoardManager 테이블로 분리
- Notification은 target_id + target_type 기반 구조 고려


| 테이블           | 설명           |
| ------------- | ------------ |
| Member        | 회원           |
| BoardCategory | 게시판 카테고리     |
| Board         | 게시판          |
| BoardManager  | 게시판 관리자      |
| Post          | 게시글          |
| PostImage     | 게시글 이미지      |
| PostVote      | 게시글 추천 / 비추천 |
| Comment       | 댓글 / 대댓글     |
| CommentLike   | 댓글 좋아요       |
| Notification  | 알림           |

---

## API 목록

### Auth

| Method | URL               | 설명     |
| ------ | ----------------- | ------ |
| POST   | /api/auth/signup  | 회원가입   |
| POST   | /api/auth/login   | 로그인    |
| POST   | /api/auth/refresh | 토큰 재발급 |

### Board

| Method | URL                   | 설명        |
| ------ | --------------------- | --------- |
| GET    | /api/boards           | 게시판 목록 조회 |
| GET    | /api/boards/{boardId} | 게시판 상세 조회 |
| POST   | /api/boards           | 게시판 생성    |
| PATCH  | /api/boards/{boardId} | 게시판 수정    |
| DELETE | /api/boards/{boardId} | 게시판 삭제    |

### Post

| Method | URL                                  | 설명        |
| ------ | ------------------------------------ | --------- |
| GET    | /api/boards/{boardId}/posts          | 게시글 목록 조회 |
| GET    | /api/boards/{boardId}/posts/{postId} | 게시글 상세 조회 |
| POST   | /api/boards/{boardId}/posts          | 게시글 작성    |
| PATCH  | /api/boards/{boardId}/posts/{postId} | 게시글 수정    |
| DELETE | /api/boards/{boardId}/posts/{postId} | 게시글 삭제    |

### Comment

| Method | URL                                                               | 설명       |
| ------ | ----------------------------------------------------------------- | -------- |
| GET    | /api/boards/{boardId}/posts/{postId}/comments                     | 댓글 목록 조회 |
| POST   | /api/boards/{boardId}/posts/{postId}/comments                     | 댓글 작성    |
| POST   | /api/boards/{boardId}/posts/{postId}/comments/{commentId}/replies | 대댓글 작성   |
| PATCH  | /api/boards/{boardId}/posts/{postId}/comments/{commentId}         | 댓글 수정    |
| DELETE | /api/boards/{boardId}/posts/{postId}/comments/{commentId}         | 댓글 삭제    |

---

## 프로젝트 구조

```text
src/main/java/com/kangyoon/community
├── domain
│   ├── member
│   ├── board
│   ├── post
│   └── comment
└── global
    ├── config
    ├── security
    ├── exception
    └── common
```

---

## 의사결정 기록

### 댓글 구조 - 트리 방식 → 플랫 방식으로 변경

초기에는 댓글과 대댓글을 트리 형태로 조립하여 응답하는 구조를 고려했습니다.

하지만 페이징 처리 과정에서 트리 구조 유지가 복잡해졌고, 실제 커뮤니티 서비스에서는 플랫 구조 기반으로 응답하는 경우가 많다는 점을 확인했습니다.

이에 QueryDSL의 `COALESCE(parent_id, id)` 정렬을 활용하여 원댓글과 대댓글 순서를 DB 레벨에서 보장하는 방식으로 변경했습니다.

결과적으로:

* 서비스 로직 단순화
* 페이징 처리 용이성 확보
* 프론트 렌더링 단순화

를 얻을 수 있었습니다.

---

### Soft Delete 적용 및 데이터 정합성 고민

게시글과 댓글 삭제 기능 구현 과정에서 물리 삭제 대신 Soft Delete 방식을 적용했습니다.

단순 삭제 처리 시:

* 댓글이 달린 게시글 삭제
* 대댓글이 존재하는 댓글 삭제
* 알림 데이터와의 관계 유지

등 운영 과정에서 데이터 정합성 문제가 발생할 수 있다고 판단했습니다.

따라서 삭제 여부를 별도 상태값으로 관리하고, 삭제된 댓글은 "삭제된 댓글입니다." 형태로 노출하는 방향을 고민하며 구현하고 있습니다.

---

## 프로젝트 목표

이번 프로젝트에서는 단순 기능 구현보다 다음과 같은 역량을 경험하는 것을 목표로 하고 있습니다.

* 유지보수 가능한 구조 설계
* 운영 환경을 고려한 데이터 처리
* 캐시 및 성능 개선 경험
* 테스트 코드 기반 개발 습관
* Git 기반 협업 흐름 경험

프로젝트를 지속적으로 개선하며 실제 서비스 운영 관점에서 고민할 수 있는 백엔드 개발자로 성장하고자 합니다.
