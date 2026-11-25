# 개발자 안내

## 프로젝트 환경

- JDK 25
- Paper 26.3
- Gradle 9.7.0
- HikariCP 기반 SQLite 연결 풀

## 주요 코드 구조

| 경로 | 역할 |
| --- | --- |
| `menu/MinigameMenu` | 참여 Dialog 화면과 버튼 처리 |
| `menu/MenuBook` | 메뉴 책 생성·지급·식별 |
| `listener/PlayerListener` | 접속·퇴장·책 사용·드롭 이벤트 |
| `game/GameSession` | 참여 명단, 다음 게임 대기열, 상태 전환, 시작·종료 |
| `game/GameState` | 대기·카운트다운·게임 진행 상태 |
| `game/GameTimer` | 카운트다운과 게임 시간 반복 작업 |
| `game/GameDisplay` | 보스바와 카운트다운 효과음 |
| `game/GameSettings` | 최소 인원·카운트다운·게임 시간 조회·저장 |
| `game/GameTeams` | 팀 배정, 상대 팀 닉네임 숨김. 참가자 개인 스코어보드에 팀·처치 수 등록과 정리 |
| `scoreboard/PlayerBoards` | 플레이어마다 개인 스코어보드 생성·적용, 종료 시 기본 스코어보드로 복원 |
| `scoreboard/ServerSidebar` | 게임 이벤트와 1초 주기로 사이드바 갱신, config의 제목·주소 사용. 모드 사용자는 제외 |
| `scoreboard/Sidebar` | 스코어보드 하나의 사이드바. 숫자 숨김, 바뀐 줄만 전송 |
| `scoreboard/SidebarLines` | 로비·게임 사이드바 문구 |
| `command/QuakeCommand` | 관리자 명령어 |
| `database/DatabaseManager` | SQLite 연결 풀 관리 |
| `database/GameWorldRepository` | 게임 월드 등록 목록 저장·조회·삭제 |
| `mod/ModHandshake` | 모드 버전 확인, 모드 사용자 기록, 수락 시 `addAcceptListener` 콜백 호출 |
| `Quake` | 구성 요소 생성·등록 및 종료 정리 |

## 스코어보드

사이드바 내용이 플레이어마다 다르기 때문에 접속한 플레이어는 모두 개인 스코어보드를 씁니다(`PlayerBoards`). 게임 팀 표시도 새 스코어보드로 바꾸지 않고, 각 참가자의 개인 스코어보드에 RED/BLUE 팀과 처치 수 objective를 등록했다가 게임이 끝나면 그것만 지웁니다. 다른 코드에서 `player.setScoreboard`를 호출하면 사이드바와 팀 표시가 사라지므로, 스코어보드가 필요하면 `PlayerBoards.boardOf(player)`에 등록합니다. 서버 기본 스코어보드(`/team`, `/scoreboard` 명령)는 플레이어 화면에 보이지 않습니다.
