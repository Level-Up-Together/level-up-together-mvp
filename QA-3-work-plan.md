# QA-3: [ADMIN] 미션북 > 생성 — 작업 계획

## Jira 이슈 요약

- **이슈**: https://pink-spider.atlassian.net/browse/QA-3
- **Confluence 스펙**: https://pink-spider.atlassian.net/wiki/spaces/LUT/pages/52232193
- **요청사항**:
  1. Confluence 스펙과 일치하도록 어드민 미션북 생성/수정 UI 정리
  2. **제거**: 주기, 1회 수행시간(분), 유형, 시작일, 종료일, 기간, 최대참여자, 완료경험치, 커스텀 가능, 상태
  3. **추가**: 카테고리 선택 드롭다운
  4. **변경**: 미션종류를 AS-IS(일간/월간/주간/1회) → TO-BE(일반/고정)으로 변경

## Confluence 스펙 — TO-BE 필드 목록

| label | example | 비고 |
|-------|---------|------|
| 카테고리 | 운동, 공부, 자기개발, 독서 등 | 카테고리 목록에서 선택 |
| 미션명 | 운동 30분 하기 | |
| 설명 | 운동을 30분이상 수행하면... | |
| 미션종류 | **TO-BE: 일반, 고정** | AS-IS: 일간/월간/주간/1회 → `is_pinned` 필드로 매핑 |
| 목표시간 | 30 | 목표시간 넘기면 보상경험치 추가 획득 |
| 보상경험치 | 10 | 목표시간 넘기면 추가 획득 |
| 공개범위 | **TO-BE: 공개, 비공개** | AS-IS: 공개/비공개/길드전용 |
| 일일 수행 제한 | 3 | |

---

## 프로젝트별 작업 현황 및 계획

### 1. Admin Frontend (`level-up-together-admin-frontend`)

**브랜치**: `develop` (수정됨, 미커밋)

#### 완료된 작업
- [x] 주기(`mission_interval`) 드롭다운 제거 — 목록 테이블, 생성 모달, 상세 페이지
- [x] 1회 수행시간(`duration_minutes`) 입력 필드 제거 — 생성 모달, 상세 페이지
- [x] 카테고리 선택 드롭다운 추가 — 생성 모달, 상세 페이지 (API로 활성 카테고리 조회)
- [x] **미션종류 UI 변경**: `is_pinned` 체크박스("고정 미션") → "미션종류" 드롭다운(`일반`/`고정`)으로 변경
  - 생성 모달 (`page.tsx` TemplateFormModal)
  - 상세 페이지 편집 모드 (`[id]/page.tsx`)
  - 상세 페이지 조회 모드 — "고정 미션: Y/N" → "미션종류: 일반/고정" 배지
  - 상세 페이지 헤더 — 고정일 때만 표시 → 항상 "일반"/"고정" 배지 표시
- [x] **목록 테이블 카테고리 컬럼**: `#${category_id}` → `category_name` 표시
- [x] **목록 테이블 미션종류 컬럼**: "고정: Y/N" → "미션종류: 일반/고정" 배지
- [x] **MissionTemplate 타입 업데이트**: `category_name: string | null` 필드 추가
- [x] **테스트 파일 수정**: `detail.test.tsx`, `page.test.tsx`에 `category_name` 필드 추가

#### 미확인 사항
- [ ] **참여방식(`participation_type`) 필드 검토**: Confluence 스펙 TO-BE에 없음, rumi 코멘트에서도 "이건 뭐임?" 의문 제기 → 제거 여부 확인 필요

#### 관련 파일
- `src/app/(afterLogin)/mission/page.tsx` — 목록 + 생성/수정 모달
- `src/app/(afterLogin)/mission/[id]/page.tsx` — 상세 + 수정 페이지
- `src/lib/api.ts` — `MissionTemplate`, `MissionTemplateCreateRequest` 타입 정의
- `src/__tests__/pages/mission/detail.test.tsx` — 상세 페이지 테스트
- `src/__tests__/pages/mission/page.test.tsx` — 목록 페이지 테스트

---

### 2. Admin Backend (`admin-service`)

**브랜치**: `develop` (클린)

#### 작업 필요 여부: 변경 없음 (Thin Proxy)

- Admin 백엔드는 Product 백엔드의 Internal API를 Feign Client로 프록시하는 구조
- `MissionTemplateRequest` DTO에서 `missionInterval`, `durationMinutes` 등은 **이미 optional** (nullable)
- 프론트에서 해당 필드를 보내지 않으면 자연스럽게 null로 전달
- `MvpApiResponse<T>`가 Product 백엔드 응답을 그대로 패스스루하므로 `category_name` 포함됨

#### 확인 결과
- [x] `MissionTemplateResponse`(admin-service 응답 DTO)에 `categoryName` 필드 이미 존재 (line 35) → 변경 불필요

#### 관련 파일
- `service/src/main/java/.../mission/api/dto/MissionTemplateRequest.java`
- `service/src/main/java/.../mission/api/dto/MissionTemplateResponse.java` (확인 필요)
- `service/src/main/java/.../mission/infrastructure/MvpMissionTemplateFeignClient.java`

---

### 3. Product Backend (`product-service`)

**브랜치**: `feature/QA-3` (커밋 완료)

#### 완료된 작업
- [x] `MissionTemplateAdminService`에 `MissionCategoryService` 연동 추가
- [x] `createTemplate()` — `categoryName` 스냅샷 저장
- [x] `updateTemplate()` — `categoryName` 스냅샷 갱신
- [x] `resolveCategoryName()` 헬퍼 메서드 추가
- [x] 테스트 통과 확인

#### 추가 작업 필요: 없음

- `MissionTemplateAdminResponse`에 `categoryName` 필드 이미 존재
- `MissionTemplateAdminRequest`에 `missionInterval`, `durationMinutes` 는 optional이므로 제거 불필요
- `is_pinned` 필드가 이미 "미션종류(일반/고정)" 개념을 수용

#### 관련 파일
- `service/mission-service/src/main/java/.../application/MissionTemplateAdminService.java` (수정 완료)
- `service/mission-service/src/main/java/.../domain/dto/MissionTemplateAdminRequest.java`
- `service/mission-service/src/main/java/.../domain/dto/MissionTemplateAdminResponse.java`
- `service/mission-service/src/main/java/.../api/MissionTemplateAdminInternalController.java`

---

### 4. Product Frontend — React Native App (`LevelUpTogetherReactNative`)

**작업 필요 여부: 없음**

- React Native 앱은 WebView 래퍼 — 미션북 화면은 네이티브로 구현되지 않음
- 모든 미션북 UI는 Web Frontend (`level-up-together-frontend`)에서 처리

---

### 5. Product Frontend — Web App (`level-up-together-frontend`)

**브랜치**: `develop` (수정됨, 미커밋)

#### 완료된 작업
- [x] MissionBook 컴포넌트: `getIntervalText(mission_interval)` → `getMissionTypeText(is_pinned)` 변경
  - AS-IS: `DAILY → 매일`, `WEEKLY → 매주`, `MONTHLY → 매월`
  - TO-BE: `is_pinned=false → 일반`, `is_pinned=true → 고정`
- [x] 번역 키 추가 (ko/en/ar): `regular`(일반/Regular/عادي), `pinned`(고정/Pinned/مثبت)
- [x] 테스트 수정: interval 관련 테스트 → mission type(일반/고정) 테스트로 변경

#### 참고 사항
- 길드 미션 페이지(`guild/[guildId]/page.tsx`, `guild/[guildId]/mission/[missionId]/page.tsx`)에서도 `getIntervalText`를 사용하지만, 길드 미션은 QA-3 스코프 외이므로 미변경

#### 관련 파일
- `src/app/(afterLogin)/mission/components/MissionBook.tsx`
- `src/messages/ko.json`, `en.json`, `ar.json`
- `src/__tests__/pages/mission/MissionBook.test.tsx`

---

## 작업 우선순위

| 순위 | 프로젝트 | 작업 | 상태 |
|------|---------|------|------|
| 1 | Product Backend | categoryName 스냅샷 저장 | ✅ 완료 |
| 2 | Admin Frontend | 주기/수행시간 제거, 카테고리 추가 | ✅ 완료 |
| 3 | Admin Frontend | 미션종류 UI 변경 (체크박스→드롭다운) | ✅ 완료 |
| 4 | Admin Frontend | 목록 카테고리 컬럼 이름 표시 | ✅ 완료 |
| 5 | Admin Frontend | MissionTemplate 타입에 category_name 추가 | ✅ 완료 |
| 6 | Admin Backend | MissionTemplateResponse에 categoryName 확인 | ✅ 이미 존재 |
| 7 | Admin Frontend | 참여방식 필드 제거 여부 | ❓ 확인 필요 |

---

## 미확인 사항 (결정 필요)

1. **참여방식(`participation_type`) 제거 여부**
   - Confluence 스펙 TO-BE에 참여방식이 없음
   - rumi 코멘트: "근데 참여방식에 직접 참여 / 템플릿방식 이건 뭐임?"
   - `DIRECT` = 유저가 직접 수행, `TEMPLATE_ONLY` = 템플릿에서만 사용 가능
   - → 기획자/PM 확인 필요

2. **출처(`source`) 필드 유지 여부**
   - Confluence 스펙 TO-BE에 출처가 없음
   - 어드민에서 생성하면 항상 `SYSTEM`이므로 UI에서 불필요할 수 있음
   - → 기획자/PM 확인 필요
