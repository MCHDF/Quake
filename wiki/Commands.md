# 관리자 명령어

`/quake` 명령어에는 `quake.command` 권한이 필요하며 기본값은 OP입니다.

## 기본 명령어

```text
/quake get book
/quake setTimer <seconds>
```

`setTimer`에는 양의 정수만 사용할 수 있습니다.

## 게임 월드 명령어

```text
/quake world create <name>
/quake world delete <name>
/quake world tp <name>
/quake world list
/quake world lobby
/quake world players <name> <min> <max>
/quake world spawn <name> red
/quake world spawn <name> blue
/quake world status <name> ready
/quake world status <name> maintenance
/quake world status <name> disabled
```

`/quake world list` 또는 `/quake world tp`를 입력하면 월드 이동 메뉴가 열립니다. 한 줄에 월드가 9개씩 표시되며, 54개를 넘으면 페이지를 나눕니다.

관리자는 원하는 스폰 지점에 서서 `/quake world lobby`를 실행합니다. 현재 월드, 좌표, 시선 방향을 저장하며 기존 로비의 접속자를 새 지점으로 이동시킵니다. 접속 시 저장된 지점으로 이동합니다. 설정이 없거나 저장된 월드가 로드되지 않았으면 기본 오버월드의 스폰을 사용합니다.

월드 이름에는 영문, 숫자, `_`, `-`만 사용할 수 있습니다. 삭제할 월드에 다른 플레이어가 없어야 하며, 현재 위치한 월드는 삭제할 수 없습니다.

`min_players`의 기본값은 2명이며 2명 또는 4명만 설정할 수 있습니다. `max_players`의 기본값은 16명이고 2~16 사이의 짝수여야 하며, 최소 인원은 최대 인원 이하여야 합니다.

```text
/quake world players desert_arena 4 12
```
