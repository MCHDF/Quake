# 관리자 명령어

`/quake` 명령어에는 `quake.command` 권한이 필요하며 기본값은 OP입니다.

## 기본 명령어

```text
/quake get book
/quake setTimer <seconds>
```

`setTimer`에는 1~180 사이의 정수(초)를 입력할 수 있습니다.

## 플레이어 명령어

`/settings`(별칭 `/설정`)는 모든 플레이어가 쓸 수 있습니다(`quake.settings`, 기본 허용). 내 설정 화면을 엽니다.

## 게임 월드 명령어

```text
/quake world create <name>
/quake world delete <name>
/quake world tp
/quake world lobby move
/quake world lobby set [name]
/quake world lobby clear
/quake world players <min> <max>
/quake world spawn red
/quake world spawn blue
/quake world status ready
/quake world status maintenance
/quake world status disabled
```

`/quake world tp`를 입력하면 월드 이동 메뉴가 열립니다. 기존 `/quake world list`도 같은 메뉴를 여는 별칭입니다. 한 줄에 월드가 9개씩 표시되며, 54개를 넘으면 페이지를 나눕니다.

관리자는 원하는 스폰 지점에 서서 `/quake world lobby move`를 실행합니다. `/quake world lobby`와 `/quake world lobby set`도 같은 동작을 수행합니다. `set <name>`에 이름을 입력하면 현재 월드 이름과 일치해야 합니다.

등록된 게임 월드에서는 채팅의 **[확인]**을 눌러 지정합니다. READY 상태인 게임 월드는 지정할 수 없습니다. 이전하면 현재 월드, 좌표, 시선 방향을 저장하고, 진행 중인 경기에 참여 중인 플레이어를 제외한 기존 로비의 접속자를 새 스폰으로 이동시킵니다. 접속 시 저장된 지점으로 이동합니다.

`/quake world lobby clear`는 로비 설정을 초기화합니다. 설정이 없거나 저장된 월드가 로드되지 않았으면 기본 오버월드의 스폰을 사용합니다. 지정된 로비 월드를 삭제하거나 READY 상태로 변경하려면 먼저 로비를 이전하거나 초기화해야 합니다.

월드 이름에는 영문, 숫자, `_`, `-`만 사용할 수 있습니다. 삭제할 월드에 다른 플레이어가 없어야 하며, 현재 위치한 월드는 삭제할 수 없습니다.

`players`, `spawn`, `status`는 명령을 실행한 현재 게임 월드에 적용됩니다.

`min_players`의 기본값은 2명이며 2명 또는 4명만 설정할 수 있습니다. `max_players`의 기본값은 16명이고 2~16 사이의 짝수여야 하며, 최소 인원은 최대 인원 이하여야 합니다.

```text
/quake world players 4 12
```
