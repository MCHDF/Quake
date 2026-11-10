# Quake

Paper 서버용 미니게임 플러그인입니다. 현재 참여 메뉴와 시작 전 카운트다운을 구현했으며, 실제 게임 진행은 개발 중입니다.

이 프로젝트는 OpenAI Codex와 함께 개발하고 있습니다.

## 환경

- JDK 25 · Paper 26.3 · Gradle 9.7.0 (Wrapper 포함)

## 기능

- 접속 시 메뉴 책 지급, 우클릭으로 Dialog 참여 메뉴 열기
- 참여 인원 보스바 및 30초 카운트다운 표시 (테스트용 최소 인원 2명)
- 참여 취소·접속 종료 시 카운트다운 초기화
- 일반 유저의 메뉴 책 드롭 제한
- OP 전용 `/quake get book` 명령어로 책 재지급

## 빌드 및 실행

```bash
./gradlew build      # 빌드
./gradlew runServer  # 개발 서버 실행
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다.

빌드 결과: `build/libs/OneShot-MCHDF.jar` — Paper 서버의 `plugins` 폴더에 넣어 사용합니다.
