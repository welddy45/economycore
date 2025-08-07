# CoreEconomy

Это личный проект, созданный в качестве упражнения по разработке плагинов для Minecraft. Основная цель — спроектировать и реализовать простую, но надежную экономическую систему с акцентом на чистую архитектуру, асинхронную обработку данных и предоставление стабильного API для других плагинов.

Плагин не претендует на конкуренцию с существующими решениями и используется для тестирования и отработки инженерных подходов.

## Функционал

*   **Управление балансом:** Основные операции с балансом игроков (просмотр, установка, выдача, снятие).
*   **Переводы между игроками:** Команда `/pay` с настраиваемой комиссией.
*   **Хранение данных:** Поддержка двух типов хранилищ: `YAML` и `SQLite`.
*   **Администрирование:** Полный набор команд для управления экономикой сервера.
*   **История транзакций:** Логирование всех изменений баланса (доступно для администраторов через команду).
*   **Таблица лидеров:** Команда `/baltop` для просмотра самых богатых игроков.
*   **API для разработчиков:** Сервис для интеграции с другими плагинами.
*   **Система бэкапов:** Автоматическое резервное копирование базы данных при использовании SQLite.
*   **Локализация:** Все сообщения вынесены в отдельный файл `messages_ru.yml`.

## Команды

### Команды для игроков (по умолчанию `/edu`)

*   `/edu balance [игрок]` — Посмотреть свой или чужой баланс.
*   `/edu pay <игрок> <сумма>` — Перевести средства другому игроку.
*   `/edu baltop` — Показать топ игроков по балансу.
*   `/edu sbtoggle` — Включить или выключить отображение скорборда.

### Команды для администраторов (по умолчанию `/eduadmin`)

*   `/eduadmin <set|add|remove> <игрок> <сумма>` — Управление балансом игрока.
*   `/eduadmin <freeze|unfreeze> <игрок>` — Заморозить/разморозить счет игрока.
*   `/eduadmin history <игрок> [страница]` — Показать историю транзакций игрока.
*   `/eduadmin reload` — Перезагрузить конфигурацию плагина.
*   `/eduadmin total` — Показать общий баланс всех игроков на сервере.

## Права (Permissions)

*   `coreeconomy.command.balance` — Доступ к `/edu balance`.
*   `coreeconomy.command.pay` — Доступ к `/edu pay`.
*   `coreeconomy.command.baltop` — Доступ к `/edu baltop`.
*   `coreeconomy.command.sbtoggle` — Доступ к `/edu sbtoggle`.
*   `coreeconomy.command.admin` — Доступ ко всем командам `/eduadmin`.

## Пример использования API

Плагин предоставляет API через `ServicesManager` от Bukkit.

```java
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.corearchitect.coreeconomy.api.EconomyAPI;
import java.util.UUID;

// ...

public void getPlayerBalance(UUID playerUUID) {
    RegisteredServiceProvider<EconomyAPI> provider = getServer().getServicesManager().getRegistration(EconomyAPI.class);
    if (provider == null) {
        // Экономический плагин не найден или не загружен
        return;
    }

    EconomyAPI economyAPI = provider.getProvider();

    economyAPI.getBalance(playerUUID).thenAccept(balance -> {
        String formattedBalance = economyAPI.format(balance);
        // Далее можно использовать отформатированный баланс
        System.out.println("Баланс игрока: " + formattedBalance);
    });
}
```

## Сборка из исходного кода

Для сборки проекта необходимы:
*   Java 17 или выше
*   Maven

Выполните команду в корневой директории проекта:
```shell
mvn clean package
```
Готовый `.jar` файл будет находиться в папке `target/`.
