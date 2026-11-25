# 개발 계획

> 원본은 노션 페이지입니다: https://app.notion.com/p/422eed1c42178107a24fccb33d31f8ad
> 이 파일은 코드 세션이 참고할 수 있도록 요약한 사본입니다.

## 방향

서버 로직은 플러그인이, 커스텀 GUI는 클라이언트 모드가 맡는 "씬 클라이언트" 구조로 만듭니다.
플러그인과 모드는 플러그인 메시지 채널(커스텀 페이로드 패킷)로 통신합니다. 채널 이름은 `quake:<경로>` 형식입니다.

## UI 방식 (미정)

| 화면 | 후보 |
| --- | --- |
| 장비 인벤토리 | 모드 / 상자 GUI + 리소스팩 |
| 상점 | 모드 / 상자 GUI + 리소스팩 |
| 특수 설정 | 모드 / Dialog (참여 메뉴는 이미 Dialog 사용) |
| HUD (체력, 아머, 탄약) | 모드 / 보스바·액션바 + 커스텀 폰트 |

## 패킷 설계 (초안)

핸드셰이크 채널은 `quake:handshake`이고 양방향에 같은 채널을 씁니다. 모드는 서버가 이 채널을 등록했을 때만 핸드셰이크를 보내고, 수락 응답을 받아야 활성화됩니다. 프로토콜 버전은 플러그인 `mod/ModProtocol`과 모드 `network/QuakeProtocol`에 함께 적습니다.

| 패킷 | 방향 | 내용 |
| --- | --- | --- |
| `HANDSHAKE` | 클라 → 서버 | 모드 프로토콜 버전 (int) — 구현됨 |
| `HANDSHAKE` 응답 | 서버 → 클라 | 서버 프로토콜 버전 (int), 수락 여부 (boolean) — 구현됨 |
| `OPEN_SCREEN` | 서버 → 클라 | 화면 종류, 대상 플레이어, 표시할 데이터 전체 |
| `ACTION` | 클라 → 서버 | 사용자 행동 |
| `UPDATE` | 서버 → 클라 | 바뀐 항목만 부분 갱신 |
| `CLOSE_SCREEN` | 서버 → 클라 | 화면 닫기 |

## 모드 대비 코드 정리 (플러그인 단계에서 진행)

플러그인을 먼저 완성하되, 모드를 붙일 때 기존 코드를 다시 고치지 않도록 미리 정리합니다.
모드에서 온 요청도 지금의 Dialog·명령어와 **같은 메서드**를 호출하게 만드는 것이 핵심입니다.
(2026-11-24 코드 기준으로 확인한 항목)

| 번호 | 항목 | 현재 코드 | 바꿀 방향 | 우선순위 |
| --- | --- | --- | --- | --- |
| R1 | 처리 결과와 메시지 분리 (완료 2026-11-24) | `GameSession`의 `join`, `requestLeave`, `leave`가 판단과 동시에 `player.sendMessage` 호출 | 결과를 enum/record로 반환(예: `JoinResult.NO_READY_WORLD`), 문구 출력은 화면 쪽이 담당 | 높음 |
| R2 | 상태 스냅샷 제공 (완료 2026-11-24) | `MinigameMenu`가 `isRunning`, `contains`, `isQueued`, `getCurrentWorld` 등을 하나씩 조회해 문구 조합 | `LobbyStatus` 같은 record 하나로 상태 제공, Dialog와 모드가 같은 데이터 사용 | 높음 |
| R3 | 게임 데이터는 플러그인이 보관 (완료 2026-11-24) | 처치 수를 바닐라 스코어보드 objective(`PLAYER_KILL_COUNT`)에 저장 | 처치 수·팀 점수를 플러그인 내부 데이터로 관리, 스코어보드·탭 목록은 표시 용도로만 사용 | 높음 |
| R4 | 게임 이벤트 알림 구조 (완료 2026-11-24) | 화면(보스바 등)이 게임 상태를 직접 조회 | 상태 변경·처치·점수·타이머 이벤트를 발행하고 `GameDisplay` 등이 구독. 나중에 모드 패킷 전송도 구독자로 추가 | 중간 |
| R5 | 아이템 문자열 id (완료 2026-11-24) | 레일건을 재료(`IRON_HOE`)와 테스트 키로 구분 | PDC에 `quake:item_id = railgun` 같은 id 저장, 장비·상점 품목도 같은 id 체계 사용 | 중간 |

R1~R5 결과 (모드를 붙일 때 쓰는 진입점):
- 요청: `GameSession.join` → `JoinResult`, `requestLeave` → `LeaveResult`
- 상태 조회: `GameSession.lobbyStatus(player)` → `LobbyStatus`
- 게임 데이터: `GameTeams.getKills`, `getTeamScore` (스코어보드는 표시용)
- 알림: `GameSession.addListener(GameListener)`로 `GameEvent` 구독. 지금 구독자는 `GameDisplay`(보스바), `GameChatNotifier`(채팅)
- 아이템: `QuakeItems.idOf(item)`, id는 `railgun`, `railgun_test`, `menu_book`

## 개발 단계

1. 기획 확정 (게임 규칙, 장비 목록) — 진행 중
2. 플러그인 핵심 로직 — 진행 중
3. 모드 대비 코드 정리 (R1~R5, 2단계와 병행 가능) — 완료
4. 플러그인 ↔ 모드 통신 기반 (핸드셰이크, 공용 모듈) — 핸드셰이크 완료 (2026-11-25)
5. 첫 모드 화면 (통신 구조 검증용)
6. 플레이어용 화면 (장비, 상점, 설정, HUD)
7. 모드 미설치 플레이어용 폴백 UI (필요하면)
8. 테스트와 밸런스 조정

## 결정해야 할 것

- [x] 버전: Paper 26.3
- [x] 모드 로더: Fabric (2026-11-25, `quake-mod` 저장소)
- [ ] 모드를 필수로 할지, 선택 설치로 할지
- [ ] 패킷 포맷: JSON / 바이너리
- [ ] 승리 조건, 리스폰 규칙
