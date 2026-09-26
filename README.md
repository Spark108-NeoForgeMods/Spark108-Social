# Spark108-Social

Клиентский и серверный мод для Minecraft 1.21.1 / NeoForge. На сервере требуются Spark108-EconomyApi и LuckyPerms 5.4 или новее.

При наведении на другого игрока появляется зелёная обводка модели. ПКМ по игроку открывает меню действий с пунктом «Перевести игровую валюту». Экран перевода показывает баланс отправителя, позволяет указать сумму и выполнить перевод через `SparkEconomyApi.transfer`. Обе формы рисуются поверх размытого мира.

`HaloApi.register(id, provider)` позволяет серверным модам передавать произвольный список подписей для ореола. Каждое поле задаёт `HaloPosition`: `HEAD_LEFT`, `HEAD_RIGHT`, `BODY_LEFT`, `BODY_RIGHT`, `LEGS_LEFT` или `LEGS_RIGHT`. Social проверяет расстояние до игрока, запрашивает поля при наведении и рисует их без фона. Провайдер сам проверяет права зрителя. Social не зависит от Essentials; модуль `whois` в Essentials регистрирует провайдер только при наличии Social.

В меню также есть обмен предметами. Первый выбор пункта отправляет предложение, о котором оба игрока получают сообщения в чате. Чтобы принять, второй игрок должен в течение 30 секунд выбрать «Обменяться предметами» в меню инициатора. До принятия окна трейда не открываются. Если предложение не принято или игроки становятся недоступны, инициатор получает сообщение об отказе. Повторное нажатие инициатора не продлевает срок; у игрока может быть только одно ожидающее предложение.

После принятия каждый участник видит своё предложение слева и предложение другого игрока справа. Оба подтверждают обмен независимо; после второго подтверждения идёт отсчёт 5 секунд. В это время любой игрок может снять подтверждение. Изменение предложения сбрасывает оба подтверждения и отсчёт. При закрытии окна, отключении игрока или удалении участников друг от друга более чем на 6 блоков предметы возвращаются владельцам.

Сервер проверяет, что получатель онлайн, находится в том же мире на расстоянии не более 6 блоков и не совпадает с отправителем. Сумма проверяется на сервере. Клиентская подсветка не меняет состояние игрока на сервере.

Права LuckyPerms (по умолчанию обе функции недоступны):

- `ru.spark108.social.transfer` — просмотр баланса и перевод валюты. Право нужно отправителю.
- `ru.spark108.social.trade` — обмен предметами. Право нужно обоим участникам.

При открытии меню сервер сообщает клиенту доступные действия; кнопки без прав не показываются. Права также проверяются при запросах на перевод, открытии и подтверждении трейда, а во время обмена — перед завершением.

Настройка `config/spark108/social/config.yml`: `allow_spectator_interaction: false` по умолчанию. При `true` наблюдатель может открыть меню, переводить валюту и обмениваться предметами при наличии обычных прав. Исключение для нажатий на слоты действует только внутри трейда Social.

Выравнивание каждого блока подписей задаётся на сервере и передаётся клиентам при входе:

```yaml
halo_labels:
  head_left: contour
  head_right: contour
  body_left: contour
  body_right: contour
  legs_left: contour
  legs_right: contour
```

Допустимые значения: `contour` — по контуру игрока, `center` — по центру блока, `left` и `right` — по соответствующему краю блока. Для трёх последних режимов Social берёт самый выступающий участок модели в пределах соответствующего блока.

Если используется интеграция с Essentials, сначала соберите Social, затем Essentials. JAR появляется в соседней папке `artifacts`.

## Регистрация полей Halo

Регистрируйте провайдер один раз при инициализации внешнего мода. Первый аргумент — уникальная строка вида `my_mod:player_info`, второй — функция, возвращающая текущие поля для зрителя `viewer` и игрока `target`. Функция вызывается на серверном потоке при обновлении подписей, поэтому значения можно вычислять заново без повторной регистрации. Если данных нет или зрителю запрещён доступ, возвращайте `List.of()`.

### Подписи по сторонам головы с проверкой LuckyPerms

```java
import java.util.List;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.server.level.ServerPlayer;
import ru.spark108.social.HaloApi;
import ru.spark108.social.HaloPosition;

public final class SocialHaloIntegration {
    public static void register() {
        HaloApi.register("my_mod:player_info", (viewer, target) -> {
            if (!hasPermission(viewer, "my_mod.player_info")) return List.of();
            return List.of(
                    new HaloApi.Field(HaloPosition.HEAD_LEFT,
                            "Уровень", Integer.toString(target.experienceLevel), 0x55FF55),
                    new HaloApi.Field(HaloPosition.HEAD_RIGHT,
                            "Здоровье", Float.toString(target.getHealth()), 0xFF5555));
        });
    }

    private static boolean hasPermission(ServerPlayer viewer, String node) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(viewer.getUUID());
            return user != null && user.getCachedData().getPermissionData()
                    .checkPermission(node).asBoolean();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
```

Проверяется право зрителя, а не игрока, на которого он смотрит. Social сам не назначает права внешним провайдерам: проверка находится в вашем коде.

### Поля у тела и ног, меняющийся набор данных

Внутри отдельного класса интеграции можно зарегистрировать ещё один провайдер:

```java
HaloApi.register("my_mod:player_status", (viewer, target) -> {
    if (!hasPermission(viewer, "my_mod.player_info")) return List.of();
    var fields = new java.util.ArrayList<HaloApi.Field>();
    fields.add(new HaloApi.Field(HaloPosition.BODY_LEFT,
            "Броня", Integer.toString(target.getArmorValue()), 0xAAAAAA));
    fields.add(new HaloApi.Field(HaloPosition.BODY_RIGHT,
            "Голод", Integer.toString(target.getFoodData().getFoodLevel()), 0xFFAA00));
    if (target.isCrouching()) fields.add(new HaloApi.Field(HaloPosition.LEGS_LEFT,
            "Движение", "Крадётся", 0xFFFF55));
    if (target.isSprinting()) fields.add(new HaloApi.Field(HaloPosition.LEGS_RIGHT,
            "Движение", "Бежит", 0x55FFFF));
    return fields;
});
```

Здесь `hasPermission` — метод из первого примера. Условные поля появляются и исчезают при следующем обновлении данных. Несколько полей с одинаковой позицией образуют один блок в порядке списка. Выравнивание блока задаётся в `halo_labels`; цвет поля — RGB `0xRRGGBB`, подпись отображается как `имя: значение`. Всего передаётся до 64 полей; имя ограничено 64 символами, значение — 256, длинные строки сокращаются.

### Необязательная интеграция

В основном классе внешнего мода, при инициализации:

```java
if (net.neoforged.fml.ModList.get().isLoaded("spark108_social")) {
    SocialHaloIntegration.register();
}
```

Класс `SocialHaloIntegration` из первого примера держите отдельно от основного класса мода, чтобы без Social код API не загружался. Добавьте API в `compileOnly` и объявите необязательную зависимость в `neoforge.mods.toml` (замените `my_mod` на свой mod id):

```toml
[[dependencies.my_mod]]
modId = "spark108_social"
type = "optional"
versionRange = "[1.0.0,)"
ordering = "AFTER"
side = "SERVER"
```

Повторная регистрация с тем же идентификатором заменяет провайдер; используйте разные идентификаторы для независимых наборов данных. Social не требует клиентской установки внешнего мода для этих текстовых полей.

## Кнопки внешних модов

Внешний мод регистрирует серверную кнопку через `PlayerMenuApi.register`. Укажите уникальный `ResourceLocation`, текст, проверку доступности и обработчик. Проверка и обработчик получают отправителя и выбранного игрока и выполняются на серверном потоке. Проверка доступности должна учитывать права вашего мода; она повторяется при нажатии. Social дополнительно проверяет расстояние, мир, наличие игрока и настройку режима наблюдателя.

```java
PlayerMenuApi.register(
        ResourceLocation.fromNamespaceAndPath("my_mod", "inspect"),
        Component.translatable("my_mod.menu.inspect"),
        (viewer, target) -> MyPermissions.canInspect(viewer),
        (viewer, target) -> viewer.sendSystemMessage(
                Component.literal("Игрок: " + target.getGameProfile().getName())));
```

`MyPermissions` в примере — проверка прав внешнего мода. `Component.literal` позволяет передать готовый текст без клиентского файла переводов. Регистрируйте кнопки при инициализации мода; одинаковый идентификатор заменяет кнопку, `PlayerMenuApi.unregister(id)` удаляет её. Кнопки появляются после встроенных действий в порядке регистрации; недоступные кнопки не передаются клиенту. Нажатие закрывает меню и вызывает серверный обработчик, который может открыть собственный интерфейс. Длинное меню прокручивается колёсиком.

Для необязательной интеграции объявите Social как optional dependency с `ordering = "AFTER"` и вызывайте регистрацию только если `ModList.get().isLoaded("spark108_social")`. Код, ссылающийся на API, поместите в отдельный класс интеграции, загружаемый при наличии Social.
