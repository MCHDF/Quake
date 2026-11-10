# Quake

Paper 서버용 미니게임 플러그인입니다. 현재 참여 메뉴와 시작 전 카운트다운을 구현했으며, 실제 게임 진행은 개발 중입니다.

## 이 프로젝트는 OpenAI Codex와 함께 개발하고 있습니다.

## 환경

- JDK 25 · Paper 26.3 · Gradle 9.7.0 (Wrapper 포함)

## 기능

- 접속 시 메뉴 책 지급, 우클릭으로 Dialog 참여 메뉴 열기
- 참여 인원 보스바 및 30초 카운트다운 표시 (테스트용 최소 인원 2명)
- 참여 취소·접속 종료 시 카운트다운 초기화
- 일반 유저의 메뉴 책 드롭 제한
- OP 전용 `/quake get book` 명령어로 책 재지급
- OP 전용 `/quake setTimer <seconds>` 명령어로 게임 시간 설정 (기본 180초, 양의 정수). 다음 게임부터 적용되며 서버 재시작 후에도 유지됩니다.
- 게임 시작 시 RED/BLUE 팀 무작위 균등 배정 (최소 1:1, 홀수 인원은 한 명 차이). 상대 팀의 머리 위 닉네임을 숨기고 종료·퇴장 시 기존 스코어보드를 복원합니다.

## 코드 구조

- `menu/MinigameMenu`: 참여 Dialog 화면과 버튼 처리
- `menu/MenuBook`: 메뉴 책 생성·지급·식별
- `listener/PlayerListener`: 접속·퇴장·책 사용·드롭 이벤트 연결
- `game/GameSession`: 참여 명단, 카운트다운, 게임 타이머, 보스바, 종료 처리
- `game/GameTeams`: 팀 배정, 상대 팀 닉네임 숨김, 스코어보드 복원
- `command/QuakeCommand`: 관리자 명령어 처리
- `Quake`: 구성 요소 생성과 등록, 종료 시 정리

## 빌드 및 실행

```bash
./gradlew build      # 빌드
./gradlew runServer  # 개발 서버 실행
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

빌드 결과: `build/libs/OneShot-MCHDF.jar` — Paper 서버의 `plugins` 폴더에 넣어 사용합니다.
