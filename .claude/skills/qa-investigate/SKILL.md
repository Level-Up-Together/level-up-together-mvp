---
name: qa-investigate
description: "QA 이슈를 조사하고 근본 원인을 분석합니다. Jira 이슈 URL/키를 받아 이슈 내용을 확인하고, 관련 코드를 추적하여 원인과 수정 방안을 제시합니다. 'QA 조사', 'QA-XX 확인', '버그 분석' 요청 시 사용합니다."
argument-hint: "<Jira issue key or URL> (e.g., QA-10, https://pink-spider.atlassian.net/browse/QA-10)"
allowed-tools: Agent, Read, Grep, Glob, Bash, mcp__plugin_atlassian_atlassian__getJiraIssue, mcp__plugin_atlassian_atlassian__searchJiraIssuesUsingJql, mcp__plugin_atlassian_atlassian__addCommentToJiraIssue, mcp__postgres-user-db__query, mcp__postgres-mission-db__query, mcp__postgres-guild-db__query, mcp__postgres-feed-db__query, mcp__postgres-gamification-db__query, mcp__postgres-meta-db__query, mcp__postgres-notification-db__query
---

# QA Issue Investigation

대상 이슈: $ARGUMENTS

## 1단계: 이슈 확인

Jira에서 이슈 정보를 가져옵니다.
- 이슈 키(예: QA-10)를 `getJiraIssue`로 조회
- URL이 주어진 경우 키를 추출하여 조회
- 이슈 제목, 설명, 우선순위, 첨부파일, 댓글을 확인

## 2단계: 영향 범위 파악

이슈 설명을 기반으로 영향받는 시스템 계층을 판단합니다.

| 키워드 | 조사 대상 |
|--------|----------|
| [APP], 앱, 모바일 | React Native (`LevelUpTogetherReactNative/`) + Web Frontend (`level-up-together-frontend/`) |
| [WEB], 웹, 프론트 | Web Frontend (`level-up-together-frontend/`) |
| [ADMIN], 어드민 | Admin Frontend (`level-up-together-admin-frontend/`) + Admin Backend (`admin-service/`) |
| [API], 서버, 백엔드 | Product Backend (`product-service/`) |
| DB, 데이터 | SQL Scripts (`level-up-together-sql/`) + DB 직접 조회 |

하이브리드 앱 이슈([APP])는 반드시 RN과 Web Frontend 양쪽 모두 조사합니다.

## 3단계: 코드 추적

Agent 도구를 활용하여 관련 코드를 **병렬로** 조사합니다.

조사 항목:
1. **관련 API 엔드포인트** - Controller, Service, Repository 추적
2. **관련 프론트엔드 코드** - 페이지, 컴포넌트, API 호출 코드
3. **이벤트/Saga 흐름** - 비동기 처리가 관련된 경우
4. **설정값** - application.yml, 환경변수, Config Server 설정

각 프로젝트 경로:
- Product Backend: `/Users/pink-spider/Code/github/Level-Up-Together/product-service`
- Admin Backend: `/Users/pink-spider/Code/github/Level-Up-Together/admin-service`
- Web Frontend: `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-frontend`
- Admin Frontend: `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-admin-frontend`
- React Native: `/Users/pink-spider/Code/github/Level-Up-Together/LevelUpTogetherReactNative`
- SQL Scripts: `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-sql`
- Config Repository: `/Users/pink-spider/Code/github/Level-Up-Together/config-repository`

## 4단계: 근본 원인 분석

코드 추적 결과를 종합하여:
1. **근본 원인** (Root Cause) - 왜 이 버그가 발생하는지 코드 레벨에서 설명
2. **재현 조건** - 어떤 상황에서 발생하는지
3. **영향 범위** - 어떤 사용자/기능에 영향을 미치는지

## 5단계: 수정 방안 제시

구체적인 수정 방안을 제시합니다:
- 수정해야 할 파일 목록과 변경 내용
- 수정의 위험도 (다른 기능에 영향이 있는지)
- 테스트 방법

## 결과 출력 형식

```
## QA 이슈 조사 결과

### 이슈 정보
- **키**: {issue-key}
- **제목**: {summary}
- **우선순위**: {priority}
- **보고자**: {reporter}

### 근본 원인
{코드 레벨에서의 원인 설명}

### 영향 범위
- 영향받는 서비스: {services}
- 영향받는 사용자: {user scope}

### 수정 방안
| # | 파일 | 변경 내용 | 위험도 |
|---|------|----------|--------|
| 1 | App.tsx | WebView URL을 ref로 고정 | LOW |

### 테스트 체크리스트
- [ ] {test item 1}
- [ ] {test item 2}
```

## 6단계: 수정 실행 여부 확인

분석 결과를 사용자에게 보여주고, 수정을 진행할지 확인합니다.
사용자가 동의하면 코드 수정을 진행합니다.
