---
name: create-skill
description: "새 Claude Code 스킬을 생성합니다. 기존 스킬 패턴을 분석하고 프로젝트 컨벤션에 맞는 SKILL.md를 생성합니다. '스킬 만들어', 'skill 생성', '새 스킬' 요청 시 사용합니다."
argument-hint: "<skill name and purpose> (e.g., 'kafka-expert Kafka 메시징 전문가')"
allowed-tools: Agent, Read, Write, Grep, Glob, Bash
---

# Skill Creator

요청: $ARGUMENTS

## 1단계: 요구사항 파악

사용자에게 다음을 확인한다:
- **스킬 이름**: kebab-case (예: `spring-boot-expert`, `qa-investigate`)
- **스킬 목적**: 어떤 작업을 수행하는 스킬인지
- **트리거 키워드**: 어떤 요청에서 이 스킬이 활성화되어야 하는지

## 2단계: 기존 스킬 패턴 분석

`.claude/skills/` 디렉토리의 기존 스킬들을 참고하여 패턴을 파악한다.

### 기존 스킬 목록 (8개)

| 스킬 | 용도 | 특징 |
|------|------|------|
| `spring-boot-expert` | 백엔드 전문가 | 핵심 규칙 5개 + 작업 흐름 (구현/디버깅/테스트) |
| `qa-investigate` | QA 이슈 조사 | 6단계 조사 절차 + Jira/DB MCP 도구 |
| `arch-review` | 아키텍처 규칙 검사 | 5가지 규칙 자동 검사 |
| `aws-expert` | AWS 인프라 | Terraform, EC2, RDS 등 |
| `postgres-expert` | DB 전문가 | 쿼리 최적화, 스키마 설계 |
| `nextjs-expert` | 프론트엔드 | Next.js, 컴포넌트, API 연동 |
| `react-native-expert` | 모바일 앱 | WebView, 네이티브 브릿지 |
| `fullstack-sync` | 풀스택 동기화 | 백엔드↔프론트엔드 연동 |

### 작업 로그 분석 (`prompt_log/claude_log_*.log`)

필요 시 로그 파일을 분석하여 해당 스킬이 필요했던 실제 작업 패턴을 추출한다.

```
prompt_log/claude_log_YYYY-MM-DD.log
형식: [HH:MM:SS] USER: ... / [HH:MM:SS] CLAUDE: ...
```

## 3단계: SKILL.md 생성

### Frontmatter 규칙

```yaml
---
name: {kebab-case-name}
description: "{한글 설명}. {트리거 키워드들} 요청 시 사용합니다."
argument-hint: "<인자 설명>"
allowed-tools: {필요한 도구들}
---
```

**allowed-tools 선택 가이드:**

| 도구 | 언제 포함 |
|------|----------|
| `Read, Grep, Glob` | 코드 분석이 필요한 모든 스킬 (기본) |
| `Edit, Write` | 코드 수정/생성이 필요한 스킬 |
| `Bash` | 빌드, 테스트 실행, 시스템 명령이 필요한 스킬 |
| `Agent` | 병렬 탐색이나 서브태스크 위임이 필요한 스킬 |
| `mcp__plugin_atlassian_*` | Jira/Confluence 연동 필요 시 |
| `mcp__postgres-*-db__query` | DB 직접 조회 필요 시 |

**description 작성 규칙:**
- 첫 문장: 스킬이 하는 일 (한글)
- 마지막 문장: 트리거 키워드 나열 (예: "'Spring', '백엔드', 'API 구현' 요청 시 사용합니다.")
- Claude가 자동으로 스킬을 매칭할 수 있도록 구체적 키워드 포함

### 본문 구조 패턴

```markdown
# {스킬 제목}

작업 내용: $ARGUMENTS

## 프로젝트 컨텍스트
{이 스킬이 다루는 프로젝트 영역의 구조와 경로}

## 핵심 규칙
{반드시 지켜야 할 규칙들 - 코드 컨벤션, 아키텍처 규칙 등}

## 작업 절차
{단계별 작업 흐름 - 분석 → 구현 → 검증}

## 결과 출력 형식 (선택)
{정형화된 결과가 필요한 경우}
```

### 프로젝트 경로 참조 (필요 시 포함)

```
- Product Backend: /Users/pink-spider/Code/github/Level-Up-Together/product-service
- Admin Backend:   /Users/pink-spider/Code/github/Level-Up-Together/admin-service
- Web Frontend:    /Users/pink-spider/Code/github/Level-Up-Together/level-up-together-frontend
- Admin Frontend:  /Users/pink-spider/Code/github/Level-Up-Together/level-up-together-admin-frontend
- React Native:    /Users/pink-spider/Code/github/Level-Up-Together/LevelUpTogetherReactNative
- SQL Scripts:     /Users/pink-spider/Code/github/Level-Up-Together/level-up-together-sql
- Config Server:   /Users/pink-spider/Code/github/Level-Up-Together/config-repository
- Platform:        /Users/pink-spider/Code/github/Level-Up-Together/level-up-together-platform
```

## 4단계: 파일 생성

1. 디렉토리 생성: `.claude/skills/{skill-name}/`
2. `SKILL.md` 파일 작성
3. 사용자에게 결과 보여주기

## 5단계: 검증

생성된 스킬이 올바른지 확인:
- [ ] frontmatter 필수 필드 존재 (name, description, allowed-tools)
- [ ] description에 트리거 키워드 포함
- [ ] `$ARGUMENTS` 참조가 본문에 포함
- [ ] 프로젝트 컨벤션 규칙이 반영됨
- [ ] allowed-tools가 스킬 목적에 맞게 설정됨

## 품질 기준

**좋은 스킬의 특징:**
- 명확한 트리거 조건 (description에 키워드 나열)
- 구체적인 작업 절차 (모호하지 않은 단계별 가이드)
- 프로젝트 컨벤션 내장 (트랜잭션 매니저, Facade 패턴, snake_case 등)
- 검증 단계 포함 (빌드, 테스트, 체크리스트)

**피해야 할 것:**
- 너무 범용적인 스킬 (기존 스킬과 겹침)
- 트리거 키워드 없는 description
- 프로젝트 규칙 누락 (특히 트랜잭션 매니저, cross-service boundary)
