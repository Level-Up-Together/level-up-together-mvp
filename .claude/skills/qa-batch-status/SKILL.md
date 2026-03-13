---
name: qa-batch-status
description: "QA 프로젝트의 이슈 현황을 일괄 조회합니다. 미해결 이슈 목록, 담당자별 분배, 우선순위별 정리를 보여줍니다. 'QA 현황', 'QA 목록', 'QA 이슈 정리', '이슈 현황', 'QA 대시보드' 요청 시 사용합니다."
argument-hint: "<filter> (e.g., '미해결', '이번 주', 'rumi 담당', 전체 보기는 빈 값)"
allowed-tools: Agent, Read, Grep, Glob, mcp__plugin_atlassian_atlassian__searchJiraIssuesUsingJql, mcp__plugin_atlassian_atlassian__getJiraIssue
---

# QA Batch Status

필터: $ARGUMENTS

## 1단계: 이슈 조회

### JQL 쿼리 구성

필터에 따라 JQL을 조합:

| 필터 | JQL |
|------|-----|
| 전체 (기본) | `project = QA ORDER BY created DESC` |
| 미해결 | `project = QA AND statusCategory != Done ORDER BY priority DESC` |
| 이번 주 | `project = QA AND created >= startOfWeek() ORDER BY created DESC` |
| 담당자 지정 | `project = QA AND assignee = "{이름}" ORDER BY priority DESC` |
| 특정 상태 | `project = QA AND status = "{상태}" ORDER BY created DESC` |

### 상태 카테고리 매핑

| Jira 상태 | 카테고리 | 의미 |
|----------|---------|------|
| 해야 할 일 | To Do | 미착수 |
| 진행 중 | In Progress | 작업 중 |
| 완료 | Done | 해결 완료 |

## 2단계: 결과 정리

### 2-1. 전체 현황 요약

```markdown
## QA 이슈 현황 ({조회 일시})

### 요약
| 상태 | 건수 |
|------|------|
| 해야 할 일 | {N} |
| 진행 중 | {N} |
| 완료 | {N} |
| **합계** | **{N}** |
```

### 2-2. 미해결 이슈 목록

```markdown
### 미해결 이슈 ({N}건)

| # | 키 | 제목 | 상태 | 담당자 | 우선순위 | 생성일 |
|---|-----|------|------|--------|---------|-------|
| 1 | QA-7 | [APP] 미션 > 캘린더 월이 바뀔 때 상세 표기 오류 | 해야 할 일 | rumi | Medium | 2026-03-01 |
```

### 2-3. 담당자별 분배

```markdown
### 담당자별 현황

| 담당자 | 해야 할 일 | 진행 중 | 완료 | 합계 |
|--------|-----------|---------|------|------|
| rumi | 2 | 1 | 5 | 8 |
| 미배정 | 1 | 0 | 0 | 1 |
```

### 2-4. 영향 범위별 분류

이슈 제목의 태그를 기반으로 분류:

| 태그 | 의미 | 이슈 수 |
|------|------|--------|
| [APP] | 모바일 앱 (RN + Web) | {N} |
| [WEB] | 웹 프론트엔드 | {N} |
| [API] | 백엔드 서버 | {N} |
| [ADMIN] | 어드민 | {N} |
| 태그 없음 | 미분류 | {N} |

## 3단계: 코드 연결 (선택)

미해결 이슈에 대해 관련 코드 상태를 확인:

### 브랜치 존재 여부
```bash
git branch -a | grep -i "QA-"
```

### 이미 수정된 이슈 감지
```bash
git log --all --oneline | grep -i "QA-{번호}"
```

미해결인데 이미 커밋이 있는 경우 → "수정 완료, Jira 상태 업데이트 필요" 표시

## 4단계: 우선순위 추천

미해결 이슈들을 다음 기준으로 작업 순서 추천:

1. **우선순위** (Highest > High > Medium > Low > Lowest)
2. **영향 범위** (크로스 프로젝트 > 단일 프로젝트)
3. **난이도 추정** (단순 UI > 로직 변경 > 리팩터링 > 아키텍처 변경)
4. **의존성** (다른 이슈에 블로킹되는지)

```markdown
### 추천 작업 순서

| 순서 | 이슈 | 이유 |
|------|------|------|
| 1 | QA-XX | 높은 우선순위 + 단순 수정 |
| 2 | QA-YY | 중간 우선순위 + 백엔드만 수정 |
| 3 | QA-ZZ | QA-YY 완료 후 진행 가능 |
```
