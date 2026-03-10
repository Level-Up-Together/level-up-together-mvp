---
name: nextjs-expert
description: "Next.js 프론트엔드 전문가. 페이지/컴포넌트 구현, API 연동, 인증/토큰 관리, 라우팅, 서버 컴포넌트/클라이언트 컴포넌트 구분, 국제화(i18n) 등 웹 프론트엔드 전반의 문제 해결과 구현을 수행합니다. 'Next.js', '프론트엔드', '웹', '페이지 구현', '컴포넌트' 요청 시 사용합니다."
argument-hint: "<task description>"
allowed-tools: Agent, Read, Edit, Write, Grep, Glob, Bash
---

# Next.js Frontend Expert

작업 내용: $ARGUMENTS

## 프로젝트 경로

| 프로젝트 | 경로 |
|----------|------|
| Web Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-frontend` |
| Admin Frontend | `/Users/pink-spider/Code/github/Level-Up-Together/level-up-together-admin-frontend` |

## 프로젝트 구조 (Web Frontend)

```
src/
├── app/                    # App Router 페이지
│   ├── (afterLogin)/       # 인증 필요 페이지 (home, mission, guild, feed 등)
│   ├── api/auth/           # Next.js API Routes (OAuth 콜백, 토큰 갱신)
│   ├── auth/mobile/        # RN WebView 토큰 수신 페이지
│   └── login/              # 로그인 페이지
├── components/             # 공통 컴포넌트
├── lib/
│   ├── api/fetch-client.ts # API 클라이언트 (401 자동 처리)
│   ├── auth/               # 인증 관련 (token-service, session-manager)
│   └── utils/native-bridge.ts # RN WebView 브릿지
├── messages/               # i18n 메시지 파일 (ko.json, en.json)
├── utils/                  # 유틸리티
└── __tests__/              # 테스트
```

## 핵심 패턴

### 1. API 호출
```typescript
import { get, post } from '@/lib/api/fetch-client';

// GET 요청 (자동으로 Bearer 토큰 + 401 처리)
const { data, error, success } = await get<ApiResult<ResponseType>>('/api/v1/endpoint');
if (success) {
    const result = data.value; // ApiResult에서 value 추출
}
```

### 2. 인증 체크
```typescript
import { getAccessToken } from '@/lib/auth/token-service';
import { isRunningInNativeApp } from '@/lib/utils/native-bridge';

// RN WebView 환경 감지
if (isRunningInNativeApp()) {
    // RN 전용 로직 (네이티브 기능 호출 등)
}
```

### 3. 서버/클라이언트 컴포넌트
```typescript
// 서버 컴포넌트 (기본값) — data fetching, SEO
export default async function Page() { ... }

// 클라이언트 컴포넌트 — 상태, 이벤트, 브라우저 API
'use client';
export default function InteractiveComponent() { ... }
```

### 4. i18n (국제화)
```typescript
import { useTranslations } from 'next-intl';
const t = useTranslations('mission');
// messages/ko.json의 "mission" 키 하위 메시지 사용
```

### 5. 이미지 업로드 (하이브리드)
```typescript
// 웹: 브라우저 file input 사용
// RN WebView: 네이티브 이미지 피커 호출
import { requestNativeImagePicker, isRunningInNativeApp } from '@/lib/utils/native-bridge';
```

## 작업 흐름

### 페이지/컴포넌트 구현 시
1. 기존 유사 페이지 패턴을 먼저 탐색
2. 서버/클라이언트 컴포넌트 구분 결정
3. API 호출은 `fetch-client` 사용
4. 타입은 백엔드 DTO와 일치하도록 snake_case
5. i18n 메시지 추가 (ko.json, en.json)

### 하이브리드 앱 고려사항
- **네비게이션**: RN WebView 내에서는 `window.location` 또는 Next.js `router` 사용
- **토큰 관리**: RN WebView에서는 `MobileTokenListener`가 postMessage로 쿠키 동기화
- **토큰 갱신**: RN WebView에서는 `TokenRefreshManager` 비활성화 (RN이 담당)
- **네이티브 기능**: 이미지 피커, 위치, 푸시 알림은 `native-bridge`를 통해 호출
- **로그아웃**: RN WebView에서는 `requestNativeLogoutConfirmation()` → 네이티브 Alert

### 테스트
```bash
# 타입 체크
npx tsc --noEmit

# 테스트 실행
npm test

# 특정 테스트
npm test -- --testPathPattern="mission"
```
