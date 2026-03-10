---
name: react-native-expert
description: "React Native 모바일 앱 전문가. 네이티브 기능(카메라, 위치, 푸시 알림), WebView 통합, 토큰 관리, 네이티브 브릿지 통신 등 모바일 앱 전반의 문제 해결과 구현을 수행합니다. 'React Native', 'RN', '모바일', '앱', 'WebView', '네이티브' 요청 시 사용합니다."
argument-hint: "<task description>"
allowed-tools: Agent, Read, Edit, Write, Grep, Glob, Bash
---

# React Native Expert

작업 내용: $ARGUMENTS

## 프로젝트 경로

React Native: `/Users/pink-spider/Code/github/Level-Up-Together/LevelUpTogetherReactNative`

## 프로젝트 구조

```
LevelUpTogetherReactNative/
├── App.tsx                      # 메인 앱 (WebView + 인증 분기)
├── src/
│   ├── config/index.ts          # WEB_URL, API_URL 설정
│   ├── hooks/
│   │   ├── useAuth.ts           # 인증 상태 관리 + 토큰 자동 갱신
│   │   ├── useImagePicker.ts    # 네이티브 이미지 피커
│   │   └── useLocation.ts       # 네이티브 위치 서비스
│   ├── screens/
│   │   └── LoginScreen.tsx      # 소셜 로그인 화면
│   ├── services/
│   │   ├── authService.ts       # OAuth API + 토큰 갱신 API
│   │   ├── chatService.ts       # 채팅 API
│   │   └── pushNotificationService.ts # FCM 푸시 알림
│   └── types/                   # TypeScript 타입 정의
├── android/                     # Android 네이티브
└── ios/                         # iOS 네이티브
```

## 아키텍처: 하이브리드 WebView 앱

```
┌─────────────────────────────────┐
│         React Native App        │
│  ┌───────────────────────────┐  │
│  │  LoginScreen (네이티브 UI) │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │    WebView (Next.js 앱)   │  │
│  │  ┌─────────────────────┐  │  │
│  │  │ MobileTokenListener │  │  │
│  │  │ (postMessage 수신)  │  │  │
│  │  └─────────────────────┘  │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │  네이티브 기능             │  │
│  │  - 이미지 피커             │  │
│  │  - 위치 서비스             │  │
│  │  - 푸시 알림 (FCM)        │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
```

## 핵심 패턴

### 1. RN ↔ WebView 통신

**RN → WebView** (injectJavaScript):
```typescript
webViewRef.current.injectJavaScript(`
    window.postMessage(${JSON.stringify(message)}, '*');
    true;
`);
```

**WebView → RN** (onMessage):
```typescript
// WebView 측 (Next.js)
window.ReactNativeWebView.postMessage(JSON.stringify({ type: 'logout' }));

// RN 측 (App.tsx handleMessage)
const data = JSON.parse(event.nativeEvent.data);
if (data.type === 'logout') { logout(); }
```

### 2. 메시지 타입

| 방향 | type | 용도 |
|------|------|------|
| RN → WebView | `tokenRefresh` | 토큰 갱신 후 쿠키 동기화 |
| RN → WebView | `imagePickerResult` | 이미지 피커 결과 전달 |
| RN → WebView | `currentLocationResult` | 위치 결과 전달 |
| WebView → RN | `logout` | 로그아웃 실행 |
| WebView → RN | `requestLogoutConfirmation` | 네이티브 로그아웃 확인 다이얼로그 |
| WebView → RN | `requestImagePicker` | 네이티브 이미지 피커 요청 |
| WebView → RN | `requestCurrentLocation` | 네이티브 위치 요청 |

### 3. 토큰 관리 (useAuth)

- **저장소**: AsyncStorage (`@auth_state` 키)
- **자동 갱신**: 만료 5분 전에 타이머로 자동 갱신 (`scheduleTokenRefresh`)
- **갱신 API**: `POST /jwt/reissue` (device_type: 'android' | 'ios')
- **WebView 동기화**: 갱신 후 `sendTokenToWebView()` postMessage로 쿠키 업데이트
- **WebView URL 고정**: `initialWebViewUrl` ref로 최초 로그인 시에만 URL 설정 (토큰 갱신 시 WebView 리로드 방지)
- **중복 갱신 방지**: `isRefreshingRef` 플래그

### 4. 푸시 알림

- Firebase Cloud Messaging (FCM) 사용
- 토큰 등록: 로그인 후 `pushNotificationService.registerToken()`
- 알림 클릭 네비게이션: `handleNotificationNavigation` → WebView URL 변경

## 작업 시 주의사항

1. **WebView source URI 변경 금지**: `source={{uri: webViewUrl}}`의 URL이 바뀌면 WebView가 리로드됨. 토큰 갱신은 postMessage만 사용
2. **이중 토큰 갱신 방지**: RN `useAuth`가 토큰 갱신 담당, WebView `TokenRefreshManager`는 RN 환경에서 비활성화
3. **Android 뒤로가기**: `BackHandler`로 WebView `goBack()` 연결
4. **딥링크/푸시 알림**: WebView 로드 전 알림 클릭 시 `pendingNavigation`에 저장 후 로드 완료 시 처리
5. **SSL**: 개발 환경에서만 자체 서명 인증서 허용 (`__DEV__` 체크)
