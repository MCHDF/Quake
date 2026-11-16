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
/quake world players <name> <min> <max>
/quake world spawn <name> red
/quake world spawn <name> blue
/quake world status <name> ready
/quake world status <name> maintenance
/quake world status <name> disabled
```

월드 이름에는 영문, 숫자, `_`, `-`만 사용할 수 있습니다. 삭제할 월드에 다른 플레이어가 없어야 하며, 현재 위치한 월드는 삭제할 수 없습니다.

`min_players`와 `max_players`의 기본값은 각각 2명과 16명입니다. 두 값은 2~16 사이의 짝수이며 최소 인원은 최대 인원 이하여야 합니다.

```text
/quake world players desert_arena 4 12
```
