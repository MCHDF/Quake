# 작업 기록

새 기록은 맨 위에 추가합니다. 형식: `## 날짜 · 세션` 다음에 한 일, 바뀐 결정, 다음 할 일.

## 2026-11-25 · 코드 세션

- 모드 로더를 Fabric으로 정했습니다. 모드 저장소는 `quake-mod`(github.com/MCHDF/quake-mod)이고 이 저장소와 같은 상위 폴더에 있습니다.
- 모드 템플릿 정리: 패키지를 `moe.mchdf` → `moe.mchdf.quake`로 옮기고 예제 Mixin을 지웠습니다.
- 핸드셰이크 구현 (채널 `quake:handshake`, 프로토콜 버전 1)
  - 플러그인: `mod/ModHandshake`가 모드 버전을 받아 응답하고, 버전이 맞는 플레이어를 기록합니다(`Quake.getModHandshake().hasMod(player)`). 테스트 `ModHandshakeTest`.
  - 모드: `client/QuakeServerConnection`이 서버가 채널을 등록했을 때만 핸드셰이크를 보내고, 수락 응답을 받으면 활성화, 접속이 끊기면 비활성화합니다. 모드 기능은 `QuakeServerConnection.isActive()`로 확인합니다.
  - 버전이 다르면 서버가 거절 응답을 보내고 모드는 채팅으로 안내한 뒤 꺼진 상태로 남습니다.
- 모드에 활성화 확인용 테스트 GUI 추가: Ctrl+F (`client/QuakeTestKey`, `client/screen/QuakeTestScreen`). 활성 상태면 화면을 열고, 비활성이면 액션바에 안내만 띄웁니다. 실제 GUI를 만들면 지웁니다.
- 모드 빌드에 shadow 적용: `./gradlew build`가 `quake-<버전>-all.jar`를 만들고 `local.properties`의 `modsDir`(저장소에 올리지 않음)로 복사합니다. jar에 넣을 외부 라이브러리는 `shade` 설정에 추가합니다.
- 핸드셰이크 응답이 클라에 도착하지 않던 문제 수정: Fabric `JOIN` 이벤트는 클라가 `minecraft:register`를 보내기 전에 호출되어, 서버가 채널 미등록 상태에서 보낸 응답을 Paper가 버렸습니다. 모드는 핸드셰이크를 등록 패킷 뒤로 미루고, 플러그인은 채널 등록 전이면 응답을 보관했다가 `PlayerRegisterChannelEvent`에서 보냅니다.
- 다음 할 일: 실제 클라이언트로 접속 확인, 첫 모드 화면 (통신 구조 검증용)

## 2026-11-24 · 코드 세션 (4)

- `CLAUDE.md`, `wiki/Plan.md`, `wiki/Worklog.md`를 `.gitignore`에서 빼고 저장소에 포함했습니다. 다른 컴퓨터에서 작업할 때도 같은 계획과 기록을 보기 위해서입니다.

## 2026-11-24 · 코드 세션 (3)

- R2~R5를 마쳐 모드 대비 코드 정리(R1~R5)가 끝났습니다. 커밋 `2357d89`, `1d0de6e`, `0ee38d3`, `1bb9a86`.
- R2: `GameSession.lobbyStatus(player)`가 `LobbyStatus`(진행 상태, 참여 상태 NONE/WAITING/QUEUED/PLAYING, 현재 월드, 인원)를 제공합니다. `MinigameMenu`가 이를 사용하고, 반복되던 `isRunning() && contains()`는 `isPlaying`으로 통일했습니다.
- R3: 처치 수를 `GameTeams`가 직접 보관합니다. 예전 바닐라 `PLAYER_KILL_COUNT`는 레일건 명중을 세지 못했습니다. 탭 목록 objective는 `DUMMY`로 바꿔 표시용으로만 씁니다. 빈 팀 판정도 내부 데이터로 바꿨습니다.
- R4: `GameEvent`(sealed)와 `GameListener`를 추가했습니다. `GameSession`과 `GameTeams`는 보스바·채팅을 직접 건드리지 않고 이벤트를 발행합니다. 보스바는 `GameDisplay`, 채팅은 `menu/GameChatNotifier`가 구독합니다. 모드 패킷 전송도 구독자 하나로 붙이면 됩니다.
- R5: `item/QuakeItems`가 PDC `quake:item_id`로 아이템을 구분합니다(`railgun`, `railgun_test`, `menu_book`). id 도입 전에 지급된 메뉴 책·레일건도 알아봅니다.
- 다음 할 일: 게임 안에서 전체 흐름 확인(참여, 카운트다운, 득점 알림, 종료 타이틀, 메뉴 책), 승리 조건·리스폰 규칙 기획 확정

## 2026-11-24 · 코드 세션 (2)

- R1 진행: `GameSession.join`은 `JoinResult`, `requestLeave`는 `LeaveResult`를 반환하고 더 이상 메시지를 보내지 않습니다.
- 안내 문구는 `menu/GameMessages`로 옮겼고 `MinigameMenu`가 결과를 받아 출력합니다. 모드도 같은 메서드를 호출하고 결과 enum으로 화면을 그리면 됩니다.
- `leave`(접속 종료에도 쓰임)는 원래 메시지를 보내지 않아 `boolean` 반환을 유지했습니다.
- 남은 메시지: 월드 이동 실패, 정원 초과, 게임 종료/취소, 대기열 자동 참여 안내는 여러 플레이어에게 가는 게임 이벤트 알림이라 R4(이벤트 구조)에서 함께 옮깁니다.
- 다음 할 일: R2 (`LobbyStatus` 상태 스냅샷)

## 2026-11-24 · 기획 세션 (2)

- 개발 순서 결정: 플러그인을 먼저 완성한 뒤 모드를 개발합니다.
- 모드 대비 코드 정리 항목 R1~R5를 `wiki/Plan.md`와 노션에 추가했습니다. 코드 수정은 코드 세션에서 진행합니다.
- `CLAUDE.md` 설계 원칙에 새 기능 작성 규칙을 추가했습니다.

## 2026-11-24 · 코드 세션

- 보스바 문구를 `|` 구분자만 빼고 굵게 표시하도록 바꿨습니다 (`GameDisplay.boldExceptBars`). 결과 타이틀 부제목은 그대로입니다.
- 미니게임 중 레일건 빔 바깥층을 쏜 사람의 팀 색으로 표시합니다 (RED: 빨간 유리, BLUE: 하늘색 유리). 테스트 레일건은 회색 그대로입니다.
- `GameTeams.getTeamOf`를 `public`으로 바꿨습니다 (weapon 패키지에서 사용).
- `CLAUDE.md`, `wiki/Plan.md`, `wiki/Worklog.md`를 `.gitignore`에 추가했습니다 (로컬 전용).
- 다음 할 일: 위 변경 사항을 게임 안에서 확인

## 2026-11-24 · 기획 세션

- 노션 개발 계획 페이지를 만들고 현재 구현 현황을 반영했습니다.
- 세션 간 공유를 위해 `CLAUDE.md`, `wiki/Plan.md`, `wiki/Worklog.md`를 추가했습니다.
- 다음 할 일: 승리 조건·리스폰 규칙 기획, 모드 로더 결정
