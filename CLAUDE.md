# Quake 프로젝트 안내 (Claude 공용)

이 프로젝트는 여러 Claude 세션이 함께 작업합니다.
- **기획 세션** (Claude 앱): 기획, 노션 계획 정리
- **코드 세션** (IntelliJ IDEA): 코드 작성

세션끼리 기억이 공유되지 않으므로, 아래 파일로 정보를 주고받습니다.

## 작업 규칙

1. 작업을 시작하기 전에 `wiki/Plan.md`(계획)와 `wiki/Worklog.md`(작업 기록)를 읽습니다.
2. 작업을 마치면 `wiki/Worklog.md` 맨 위에 무엇을 했는지 기록합니다.
3. 계획을 바꿔야 할 일이 생기면 `wiki/Plan.md`를 수정하고 Worklog에 이유를 남깁니다.
4. 노션 계획 페이지(Notion을 사용할 수 있는 세션만):
   https://app.notion.com/p/422eed1c42178107a24fccb33d31f8ad
   노션과 `wiki/Plan.md`의 내용이 다르면 최근 Worklog 기록을 확인하고 맞춥니다.

## 프로젝트 요약

- Paper 26.3 · JDK 25 · Gradle 9.7 · SQLite(HikariCP)
- 패키지 `moe.mchdf.quake`, 빌드 `./gradlew build`, 테스트 서버 `./gradlew runServer`
- 기존 문서: `README.md`, `wiki/Gameplay.md`, `wiki/Commands.md`, `wiki/World-Management.md`, `wiki/Development.md`

## 설계 원칙

- 게임 판정은 항상 서버(플러그인)에서 처리합니다. 클라이언트에서 온 데이터는 조작될 수 있다고 가정합니다.
- 클라이언트 모드(예정)는 화면 표시와 입력 전달만 맡습니다.
- 화면에 보여 줄 데이터는 서버가 보내 주고 모드에 하드코딩하지 않습니다.
- 상점 구매, 장비 변경 같은 동작은 화면(Dialog, 상자 GUI) 코드 안에 쓰지 않고 별도 서비스 메서드로 분리합니다. 예: `ShopService.buy(player, itemId)`
- 게임 로직은 처리 결과를 반환하고, 안내 메시지 출력은 화면 쪽 코드가 담당합니다.
- 처치 수·점수 같은 게임 데이터는 플러그인 내부에 보관하고, 스코어보드·보스바·탭 목록은 표시 용도로만 사용합니다.
- 아이템·장비는 문자열 id(예: `railgun`)로 구분합니다.
- 기존 코드의 정리 항목(R1~R5)은 `wiki/Plan.md`의 "모드 대비 코드 정리"를 참고합니다.
