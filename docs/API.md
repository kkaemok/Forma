# Forma Public API

Forma는 plugin enable 시 `FormaAPI`를 Bukkit `ServicesManager`에 등록합니다.  
외부 플러그인은 `plugin.yml`에 `depend: [Forma]` 또는 `softdepend: [Forma]`를 선언한 뒤 서비스를 조회합니다.

## API 가져오기

```java
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.kkaemok.forma.api.FormaAPI;

public final class TestPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        FormaAPI api = Bukkit.getServicesManager().load(FormaAPI.class);
        if (api == null) {
            getLogger().warning("Forma가 활성화되어 있지 않습니다.");
            return;
        }

        ItemStack sword = api.getItems().createItem("sinhyeon_ryu");
        getLogger().info("Forma 아이템 생성: " + sword.getType());
    }
}
```

Forma와 강한 의존 관계가 있는 플러그인에서는 `FormaAPI.get()`도 사용할 수 있습니다. Forma가 비활성화된 상태에서 API를 호출하면 `IllegalStateException`이 발생합니다.

## 아이템 API

```java
FormaAPI api = FormaAPI.get();

api.getItems().getItem("sinhyeon_ryu").ifPresent(data ->
        getLogger().info(data.id() + " / " + data.model()));

ItemStack gem = api.getItems().createItem("leaf_gem", 16);
boolean formaItem = api.getItems().isFormaItem(gem);
```

`createItem(id, amount)`의 수량은 해당 아이템의 최대 스택 수를 넘을 수 없습니다.

## 블럭 API

```java
ItemStack blockItem = api.getBlocks().createBlockItem("leaf_ore");

api.getBlocks().getPlacedBlock(location).ifPresent(block ->
        getLogger().info(block.id() + " -> " + block.visualState()));
```

`setPlacedBlock`과 `forceSetPlacedBlock`은 플레이어 클릭 컨텍스트가 없는 관리용 설치 API입니다. visual state와 storage 저장을 함께 적용하고, storage 저장에 실패하면 월드 변경을 롤백합니다. 플레이어 기반 보호 플러그인 검사와 설치 이벤트가 필요한 동작은 실제 Forma 블럭 아이템 설치 흐름을 사용해야 합니다.

## 레시피 API

```java
api.getRecipes().reloadRecipes();
api.getRecipes().registerItemRecipe("sinhyeon_ryu");
api.getRecipes().registerBlockRecipe("leaf_ore");
api.getRecipes().getRecipeKeys().forEach(key ->
        getLogger().info("Forma recipe: " + key));
```

`items.yml`과 `blocks.yml`의 `recipe.ingredients`에는 바닐라 `Material`, Forma 아이템 ID, Forma 블럭 ID를 사용할 수 있습니다. Forma ID 재료는 PDC까지 일치하는 커스텀 아이템만 소비하며, 기본 설정에서는 커스텀 아이템을 `EMERALD`나 `PAPER` 같은 기반 재질로 소비하는 일반 조합법을 차단합니다.

## 이벤트

### 블럭 파괴 드롭 수정

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kkaemok.forma.api.event.FormaBlockBreakEvent;

public final class FormaListener implements Listener {
    @EventHandler
    public void onFormaBlockBreak(FormaBlockBreakEvent event) {
        if (event.getBlockId().equals("leaf_ore")) {
            event.getPlayer().sendMessage("Leaf ore broken!");
            event.getDrops().clear();
        }
    }
}
```

### 지급 또는 설치 취소

```java
@EventHandler
public void onFormaGive(FormaItemGiveEvent event) {
    if (event.getItemId().equals("sinhyeon_ryu")) {
        event.setCancelled(true);
    }
}

@EventHandler
public void onFormaPlace(FormaBlockPlaceEvent event) {
    if (event.getBlockId().equals("leaf_ore")) {
        event.setCancelled(true); // 설치 변경은 Forma가 롤백합니다.
    }
}
```

제공 이벤트:

- `FormaItemGiveEvent`: `/forma get`, `/forma give`로 아이템을 지급하기 직전
- `FormaItemUseEvent`: 커스텀 아이템의 `right_click` behavior 실행 직전
- `FormaBlockPlaceEvent`: 플레이어 설치 pipeline 중 storage 저장 직전
- `FormaBlockBreakEvent`: 설치된 Forma 블럭의 파괴 및 드롭 처리 직전
