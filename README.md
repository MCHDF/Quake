# Quake

Paper 서버용 미니게임 플러그인입니다. 참여 메뉴, 월드 로테이션, 팀 배정과 게임 진행 기능을 개발하고 있습니다.

## 빠른 시작

### 요구 사항

- JDK 25
- Paper 26.3
- Gradle 9.7.0 (Wrapper 포함)

### 빌드 및 실행

```bash
./gradlew build
./gradlew runServer
```

Windows에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다. 빌드 결과물은
`build/libs/Quake-1.0.0-all.jar`이며 Paper 서버의 `plugins` 폴더에 넣습니다.

## 문서

- [게임 플레이](wiki/Gameplay.md)
- [관리자 명령어](wiki/Commands.md)
- [게임 월드 관리](wiki/World-Management.md)
- [개발자 안내](wiki/Development.md)
