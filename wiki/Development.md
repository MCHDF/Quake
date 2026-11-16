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
| `game/GameTeams` | 팀 배정, 상대 팀 닉네임 숨김, 스코어보드 복원 |
| `command/QuakeCommand` | 관리자 명령어 |
| `database/DatabaseManager` | SQLite 연결 풀 관리 |
| `database/GameWorldRepository` | 게임 월드 등록 목록 저장·조회·삭제 |
| `Quake` | 구성 요소 생성·등록 및 종료 정리 |
