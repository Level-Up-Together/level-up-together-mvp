---
name: fullstack-sync
description: "풀스택 변경사항을 동기화합니다. 백엔드 API 변경 시 프론트엔드 영향 분석, 프론트엔드 변경 시 백엔드 API 확인, 전체 스택에 걸친 기능 구현을 수행합니다. 'fullstack', '풀스택', '프론트 연동', 'API 연동', '동기화' 요청 시 사용합니다."
argument-hint: "<description of the change or feature>"
allowed-tools: Agent, Read, Edit, Write, Grep, Glob, Bash
---

# Full Stack Synchronization Expert

변경/기능 설명: $ARGUMENTS

## 프로젝트 경로

| 레이어 | 경로 | 기술 스택 |
|--------|------|----------|
| Product Backend | `/Users/pink-spider/Code/github/Level-Up-Together/product-service` | Spring Boot 3.4, Java 21 |
| Admin Backend | `/Users/pink-spider/Code/github/Level-Up-Together/admin-service` | Spring Boot 3.4, Java 21 |
| Web Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-frontend` | Next.js 15, TypeScript |
| Admin Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-admin-frontend` | Next.js 15, TypeScript |
| React Native | `/Users/pink-spider/Code/github/Level-Up-Together/LevelUpTogetherReactNative` | React Native, TypeScript |
| SQL Scripts | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-sql` | PostgreSQL |
| Config | `/Users/pink-spider/Code/github/Level-Up-Together/config-repository` | YAML |

## 작업 흐름

### 1단계: 변경 방향 파악

변경이 어디서 시작되는지 판단합니다:
- **Backend → Frontend**: API 응답 필드 추가/변경/삭제, 새 엔드포인트 추가
- **Frontend → Backend**: 프론트에서 필요한 API가 없거나 수정 필요
- **Full Feature**: 새 기능을 전 스택에 걸쳐 구현

### 2단계: 영향 분석 (병렬 수행)

Agent를 활용하여 관련 코드를 병렬로 탐색합니다:

**백엔드 변경 시 확인할 프론트엔드 항목:**
- API 호출 코드 (fetch-client, axios 등)
- TypeScript 타입/인터페이스 정의
- API 응답을 사용하는 컴포넌트
- 관련 테스트 코드

**프론트엔드 변경 시 확인할 백엔드 항목:**
- Controller 엔드포인트 및 요청/응답 DTO
- Service 비즈니스 로직
- DB 스키마 (Entity)

### 3단계: 동기화 체크리스트

변경 시 반드시 확인:

| 항목 | 설명 |
|------|------|
| **필드명 규칙** | Backend DTO: `@JsonNaming(SnakeCaseStrategy.class)`, Frontend: snake_case |
| **타입 매핑** | Java `Long` → TS `number`, Java `LocalDateTime` → TS `string`, Java `enum` → TS `string \| enum` |
| **API 응답 래핑** | Backend: `ApiResult<T>` → Frontend: `response.value`로 언래핑 |
| **에러 코드** | Backend `ApiStatus` 코드가 Frontend에서 처리되는지 확인 |
| **Null 처리** | Backend nullable 필드 → Frontend optional (`?`) 타입 |
| **날짜 형식** | Backend ISO 8601 → Frontend date parsing |

### 4단계: 코드 수정

변경 사항을 각 레이어에 반영합니다:
1. Backend: DTO, Controller, Service 수정
2. Frontend: TypeScript 타입, API 호출 코드, 컴포넌트 수정
3. SQL: 필요 시 DDL/DML 변경 스크립트 작성
4. 테스트: 양쪽 테스트 업데이트

### 5단계: 검증

- Backend: `./gradlew :service:test --tests "*.관련Controller*"` 실행
- Frontend: 타입 에러 확인 (`npx tsc --noEmit`)
- API 호환성: 요청/응답 형식이 양쪽에서 일치하는지 최종 확인
