# Forma

Paper 1.21.11 기반 서버에서 리소스팩 커스텀 아이템과 커스텀 블럭을 정의하고 관리하는 플러그인입니다.  
`items.yml`과 `blocks.yml`로 콘텐츠를 등록하고, 명령어 지급, 조합법, 자동 리소스팩 생성, 외부 플러그인 API를 제공합니다.

## 요구 사항

| 항목 | 버전 |
| --- | --- |
| 서버 | Paper 1.21.11 |
| Java | 21 |
| 빌드 | Gradle Wrapper 포함 |
| 리소스팩 포맷 | Minecraft 1.21.11 `min_format` / `max_format` `[75, 0]` |

## 주요 기능

- `item_model` 컴포넌트 기반 커스텀 아이템과 `custom-model-data` fallback
- 실제 `max_damage` / `damage` 컴포넌트 기반 커스텀 내구도
- MiniMessage 이름과 로어, glow, unbreakable, stack size, attributes
- 우클릭 아이템 behavior: 사운드, 메시지, 명령어, 쿨타임
- `SHAPED` / `SHAPELESS` 조합법과 커스텀 아이템/블럭 재료
- PAPER 기반 커스텀 블럭 설치 아이템과 별도 visual state provider
- `SOLID` / `BROWN_MUSHROOM_BLOCK` provider 및 NOTE_BLOCK legacy 호환
- 설치 위치 저장, 파괴 드롭, 설치 pipeline, 블럭별 place/break sound
- 아이템/블럭 모델과 blockstate를 포함하는 리소스팩 zip 자동 생성
- Bukkit `ServicesManager` 기반 Public API 및 취소 가능한 이벤트

## 빌드

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew clean build
```

빌드된 플러그인 jar:

```text
build/libs/forma-1.0.0.jar
```

## 설치

1. Paper 1.21.11 서버를 Java 21로 실행합니다.
2. `forma-1.0.0.jar`를 서버의 `plugins/` 폴더에 넣습니다.
3. 서버를 실행하면 `plugins/Forma/` 아래에 기본 설정 파일이 생성됩니다.
4. `items.yml`, `blocks.yml`과 리소스팩 원본 파일을 수정합니다.
5. 서버에서 `/forma reload`와 `/forma pack generate`를 실행합니다.
6. 생성된 `plugins/Forma/generated/Forma-Pack.zip`을 서버 리소스팩으로 배포합니다.

## 데이터 폴더

```text
plugins/Forma/
├─ config.yml
├─ items.yml
├─ blocks.yml
├─ blocks-data.yml                       # 설치 위치 저장, 자동 관리
├─ cache/
│  └─ visual_block_states.json           # auto provider 배정 캐시, 자동 관리
├─ resourcepack/
│  ├─ textures/
│  │  ├─ item/
│  │  └─ block/
│  └─ models/
│     ├─ item/
│     └─ block/
└─ generated/
   └─ Forma-Pack.zip
```

## 명령어와 권한

모든 관리 명령어에는 `forma.op` 권한이 필요하며 기본값은 OP입니다.

| 명령어 | 설명 |
| --- | --- |
| `/forma get <id> [amount]` | 자신에게 커스텀 아이템 지급 |
| `/forma give <player> <id> [amount]` | 대상에게 커스텀 아이템 지급 |
| `/forma list` | 등록된 아이템 목록 출력 |
| `/forma block get <id> [amount]` | 자신에게 커스텀 블럭 설치 아이템 지급 |
| `/forma block give <player> <id> [amount]` | 대상에게 커스텀 블럭 설치 아이템 지급 |
| `/forma block list` | 등록된 커스텀 블럭 목록 출력 |
| `/forma block reload` | `blocks.yml`과 저장 위치 충돌 검증 다시 수행 |
| `/forma recipe list` | 등록된 Forma 조합법 목록 출력 |
| `/forma recipe reload` | 아이템/블럭 기반 조합법 다시 등록 |
| `/forma pack generate` | 최신 설정을 읽고 리소스팩 zip 생성 |
| `/forma pack reload` | pack 설정과 입력/출력 폴더 확인 |
| `/forma reload` | config, 아이템, 블럭, 조합법, 캐시 전체 다시 로드 |

## 커스텀 아이템

권장 형식은 `item`, `settings`, `attributes`, `behaviors`, `recipe` 섹션을 나누는 구조입니다.

```yaml
sinhyeon_ryu:
  type: WEAPON

  item:
    material: NETHERITE_SWORD
    name: "<gradient:#A7F85B:#16A34A>신현류</gradient>"
    model: "forma:sinhyeon_ryu"
    custom-model-data: 1001
    lore:
      - "&7잎의 힘이 깃든 검"

  settings:
    glow: true
    unbreakable: false
    hide-attributes: false
    max-stack-size: 1
    durability:
      max: 2500
      current: 2500

  attributes:
    attack_damage: 9
    attack_speed: 1.6

  behaviors:
    right_click:
      - type: sound
        sound: "BLOCK_AMETHYST_BLOCK_CHIME"
        volume: 1.0
        pitch: 1.2
        cooldown: 3s
      - type: message
        message: "<#7CFF6B>신현류의 기운이 느껴집니다."
        cooldown: 3s
```

### 아이템 타입

| type | 용도 |
| --- | --- |
| `ITEM` | 일반 아이템 |
| `WEAPON` | 무기, handheld 기본 모델 및 내구도 지원 |
| `TOOL` | 도구, handheld 기본 모델 및 내구도 지원 |
| `ARMOR` | 방어구, 내구도 및 장비 슬롯 attribute 추론 지원 |
| `CONSUMABLE` | 향후 확장용 분류 |
| `BLOCK_ITEM` | 분류용 타입. 설치 블럭은 `blocks.yml`에서 관리 |

기존의 평평한 `material`, `name`, `model`, `lore`, `glow`, `unbreakable`, `durability`, `damage` 형식도 호환됩니다.

### 내구도

`WEAPON`, `TOOL`, `ARMOR`에 `settings.durability`를 설정하면:

```text
minecraft:max_damage = max
minecraft:damage = max - current
```

예를 들어 `max: 2500`, `current: 2400`이면 `damage: 100`입니다. PDC에도 최대/현재 내구도가 저장되며, `unbreakable: true`인 아이템에는 custom damage 컴포넌트를 적용하지 않습니다.

### Attributes

지원 값:

```yaml
attributes:
  attack_damage: 9
  attack_speed: 1.6
```

`config.yml`의 `items.replace-vanilla-attributes: true`이면 기존 바닐라 modifier를 제거하고 작성값을 플레이어가 장착했을 때의 최종 능력치로 해석합니다. 예를 들어 `attack_damage: 9`는 최종 공격력 `9`, `attack_speed: 1.6`은 최종 공격 속도 `1.6`이 되도록 내부 modifier를 각각 플레이어 기본값 `1`과 `4`에서 보정합니다. `movement_speed`도 기본 이동 속도 `0.1`을 포함한 최종값이며, 바꾸지 않을 필드는 생략합니다.

`replace-vanilla-attributes: false`이면 작성값은 기존 바닐라 attribute 위에 추가되는 `ADD_NUMBER` 보너스입니다.

## 커스텀 블럭

권장 방식은 설치 아이템과 월드의 표시용 바닐라 blockstate를 분리하는 provider 방식입니다. 설치 아이템은 `PAPER`로 만들고, 설치된 월드 블럭은 provider가 선택합니다.

```yaml
leaf_ore:
  name: "<#7CFF6B>Leaf Ore"
  item:
    material: PAPER
    model: "forma:leaf_ore"
    custom-model-data: 3001
    lore:
      - "&7잎의 힘이 담긴 광석"

  settings:
    hardness: 3.0
    tool: PICKAXE
    sounds:
      place: "BLOCK_STONE_PLACE"
      break: "BLOCK_STONE_BREAK"
      volume: 1.0
      pitch: 1.0

  state:
    provider: SOLID
    variation: auto
    model: "forma:block/leaf_ore"

  drops:
    - "leaf_gem"
```

### Visual State Provider

provider는 커스텀 블럭 외형을 연결할 실제 vanilla blockstate 공급자입니다. Forma는 설치 위치를 블럭 ID와 함께 저장하므로, 실제 월드 Material과 논리적 Forma 블럭 ID는 분리됩니다.

| provider | 실제 기반 블럭 | variation | 설명 |
| --- | --- | --- | --- |
| `SOLID` | `BROWN_MUSHROOM_BLOCK` | `1..64`, `auto` | 일반 고체 커스텀 블럭 권장값 |
| `BROWN_MUSHROOM_BLOCK` | `BROWN_MUSHROOM_BLOCK` | `1..64`, `auto` | SOLID의 기반 provider 명시 |
| `NOTE_BLOCK` | `NOTE_BLOCK` | `1..800`, `auto` | 기존 방식 호환. 노트블럭 상태 충돌 가능 |

`variation: auto`는 `cache/visual_block_states.json`에 배정 결과를 저장해 리로드나 재시작 후에도 외형 state가 바뀌지 않게 합니다.

기존 NOTE_BLOCK 형식도 계속 사용할 수 있습니다.

```yaml
leaf_log:
  type: NOTE_BLOCK
  item:
    material: PAPER
    model: "forma:leaf_log"
  block:
    model: "forma:block/leaf_log"
    variation: 2
  drops:
    - "leaf_log"
```

`TRIPWIRE`, `DISPLAY` 타입은 확장용으로 구조만 열려 있으며 현재 설치 provider로 구현되어 있지 않습니다.

## 조합법

아이템과 블럭 설치 아이템 모두 결과물로 등록할 수 있습니다.

아이템 결과물은 `items.yml`에, 블럭 결과물은 `blocks.yml`에 `recipe`를 작성합니다.

```yaml
recipe:
  type: SHAPED
  pattern:
    - "GGG"
    - "GGG"
    - "GGG"
  ingredients:
    G: leaf_gem
  amount: 1
```

```yaml
recipe:
  type: SHAPELESS
  ingredients:
    - leaf_gem
    - DIAMOND
  amount: 1
```

재료 해석:

| 작성 값 | 판정 |
| --- | --- |
| `leaf_gem` | `items.yml`의 동일 ID 커스텀 아이템만 허용 |
| `leaf_ore` | `blocks.yml`의 동일 ID 커스텀 블럭 아이템만 허용 |
| `DIAMOND`, `STICK` | Bukkit Material 아이템 허용 |

기본 설정에서는 Forma 아이템을 기반 Material로 소비할 수 없습니다. 예를 들어 `leaf_gem`이 `EMERALD` 기반이라도 `EMERALD`를 요구하는 일반 조합법의 재료로 사용할 수 없습니다.

```yaml
recipes:
  prevent-custom-items-as-vanilla-ingredients: true
```

## 리소스팩 자동 생성

입력 파일:

```text
plugins/Forma/resourcepack/
├─ textures/item/<model-id>.png
├─ models/item/<model-id>.json       # 선택, 없으면 기본 모델 자동 생성
├─ textures/block/<block-id>.png
└─ models/block/<block-id>.json      # 선택, 없으면 cube_all 모델 자동 생성
```

생성:

```text
/forma pack generate
```

출력 zip에는 다음과 같은 파일이 포함됩니다.

```text
pack.mcmeta
assets/forma/items/<id>.json
assets/forma/models/item/<id>.json
assets/forma/textures/item/<id>.png
assets/forma/models/block/<id>.json
assets/forma/textures/block/<id>.png
assets/minecraft/blockstates/brown_mushroom_block.json
assets/minecraft/blockstates/note_block.json
```

블럭 provider를 실제로 사용하는 경우에만 대응하는 blockstate JSON이 생성됩니다. 텍스처가 없어도 생성은 계속되며 누락 목록이 콘솔과 명령어 결과에 표시됩니다.

## 주요 설정

```yaml
namespace: forma
model-mode: ITEM_MODEL
debug: true

items:
  replace-vanilla-attributes: true
  behavior-cooldown-message: "<red>아직 사용할 수 없습니다. 남은 시간: %time%"

recipes:
  prevent-custom-items-as-vanilla-ingredients: true

custom-blocks:
  enabled: true
  remove-missing-block-data: true
  prevent-note-block-interaction: true
  prevent-redstone-update: true
  allow-replaceable-placement: true
  prevent-reserved-note-block-states: true
  reset-unregistered-reserved-note-blocks: true
  play-place-sound: true
  play-break-sound: true
  swing-hand-on-place: true
  send-game-event: true
  call-bukkit-block-place-event: true

resource-pack:
  enabled: true
  generate-blocks: true
  output-file: "Forma-Pack.zip"
  pack-version:
    min: [75, 0]
    max: [75, 0]
```

각 필드와 사용 가능한 값은 jar에 포함된 기본 [`config.yml`](src/main/resources/config.yml), [`items.yml`](src/main/resources/items.yml), [`blocks.yml`](src/main/resources/blocks.yml)에 주석으로도 설명되어 있습니다.

## Public API

Forma는 활성화 시 `FormaAPI`를 Bukkit `ServicesManager`에 등록합니다.

```java
FormaAPI api = Bukkit.getServicesManager().load(FormaAPI.class);
if (api != null) {
    ItemStack item = api.getItems().createItem("sinhyeon_ryu");
    ItemStack blockItem = api.getBlocks().createBlockItem("leaf_ore");
}
```

제공 기능:

- 아이템/블럭 아이템 생성 및 PDC 기반 판별
- 설치된 Forma 블럭 조회와 관리
- 조합법 다시 등록 및 개별 등록
- `FormaItemGiveEvent`, `FormaItemUseEvent`
- `FormaBlockPlaceEvent`, `FormaBlockBreakEvent`

자세한 연동 예시는 [`docs/API.md`](docs/API.md)를 참고하세요.

## 현재 범위와 주의사항

- 고체 블럭용 `SOLID` provider는 현재 `BROWN_MUSHROOM_BLOCK` state를 사용합니다.
- `NOTE_BLOCK` provider는 같은 바닐라 state가 일반 노트블럭에도 나타날 수 있어 완전한 시각 충돌 방지는 불가능하며, Forma가 저장한 위치만 실제 커스텀 블럭으로 처리합니다.
- 블럭의 `hardness`, `tool`, `step`, `hit`, `fall` 값은 확장을 위한 설정 데이터이며 현재 설치/파괴의 핵심 판정에는 모두 사용되지 않습니다.
- Paper 이벤트 기반 구현으로, 서버 보호 플러그인과의 호환을 위해 설치 pipeline에서 `BlockPlaceEvent` 호출 옵션을 제공합니다.

## 라이선스

이 프로젝트의 라이선스는 [`LICENSE`](LICENSE)를 확인하세요.
