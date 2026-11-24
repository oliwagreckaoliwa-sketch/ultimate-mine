# PROJECT_STATE — AutoMiner Ultimate

Ostatnia aktualizacja: 2026-11-22

## Cel
Kliencki mod Fabric dla Minecraft 1.21.1 (Java 21): Auto Miner, Block Selector (F5), Auto Deposit,
Resource Tracker, GUI w stylu dark/VibeCode (F9), HUD. Pełna specyfikacja: wejściowy plik zadania.

## Stan etapów
| Etap | Status |
|------|--------|
| 0. Sprawdzenie środowiska | ZROBIONE — patrz "Blokery" |
| 1. Szkielet (Gradle/Loom, fabric.mod.json, CI) | ZROBIONE |
| 2. Architektura | ZROBIONE |
| 3. config, keybinds, notifications, selector | NAPISANE, niezkompilowane |
| 4. mining, deposit, statistics | NAPISANE, niezkompilowane |
| 5. GUI (Ui, MainScreen, 7 zakładek, HudEditScreen) | NAPISANE, niezkompilowane |
| 6. HUD, ustawienia, integracja (AutoMinerClient) | NAPISANE, niezkompilowane |
| 7. Zasoby (lang en/pl, ikona), README, workflow GitHub Actions | ZROBIONE |
| 8. Kompilacja Gradle, naprawa błędów, testy w grze | NIEWYKONANE — brak dostępu do Maven w sesji |

Weryfikacja wykonana: `javac` (sama składnia, bez zależności) — 0 błędów parsowania; wszystkie nierozwiązane
symbole dotyczą wyłącznie klas Minecraft/Fabric/Gson/slf4j; brak odwołań do nieistniejących członków
własnych klas. To NIE jest dowód poprawności z API Minecrafta.

## Blokery
- Proxy sesji zwraca 403 dla maven.fabricmc.net, libraries.minecraft.net i services.gradle.org (sprawdzone dwukrotnie).
  `gradle build` nie przejdzie w tej sesji. Nie obchodzimy tego.
- Kompilacja: GitHub Actions (`.github/workflows/build.yml`) lub lokalnie u użytkownika.

## Struktura (pakiety `autominer.*`)
- `AutoMiner` (stałe/usługi), `AutoMinerClient` (wejście klienta, pętla ticków, zamykanie).
- `config`: ModConfig, ConfigManager. `keybinds`: KeybindManager. `notifications`: NotificationManager.
- `selector`: BlockSelector, BlockSelectionManager, BlockGroup, BlockInfo.
- `mining`: AutoMiningManager, MiningController, MiningTargetSelector, BlockWhitelist, ToolManager.
- `deposit`: AutoDepositManager, ChestManager, InventoryTransfer.
- `statistics`: StatisticsManager, SessionStatistics, ResourceCounter, InventoryWatcher.
- `hud`: AutoMinerHud (model), HudRenderer. `util`: Fmt, AimHelper.
- `gui`: Draw, Ui, GuiPanel, MainScreen, DashboardScreen, MiningScreen, BlockSelectorScreen, DepositScreen,
  StatisticsScreen, HudSettingsScreen, SettingsScreen, HudEditScreen.

## Niezweryfikowane API (pierwsze miejsca do sprawdzenia przy błędach kompilacji)
1. `gradle.properties`: wersje Loom 1.9-SNAPSHOT (build.gradle), Loader 0.16.14, Fabric API 0.116.7+1.21.1, Yarn 1.21.1+build.3.
2. `BlockSelector`: `WorldRenderEvents.AFTER_ENTITIES`, `ctx.consumers()/matrixStack()/camera()`,
   `WorldRenderer.drawBox(MatrixStack, VertexConsumer, Box, r, g, b, a)`, `Immediate.draw(RenderLayer)`.
3. `ChestManager`/`MiningController`: `PlayerEntity.getBlockInteractionRange()`.
4. `ToolManager`: `ItemStack.getMiningSpeedMultiplier/isSuitableFor`, `ItemEnchantmentsComponent.DEFAULT/getEnchantments/getLevel`,
   `RegistryEntry.matchesKey`, `Enchantments.EFFICIENCY` jako RegistryKey, klasa `PickaxeItem`.
5. `KeybindManager.remapPerspective`: `GameOptions.togglePerspectiveKey`, `KeyBinding.getBoundKeyTranslationKey/setBoundKey/updateKeysByCode`,
   `InputUtil.fromKeyCode(int,int)`, `GameOptions.write()`.
6. `ChestManager.worldKey`: `MinecraftClient.getServer().getSaveProperties().getLevelName()`, `getCurrentServerEntry().address`.
7. `HudRenderer`: podpis `HudRenderCallback` (DrawContext, RenderTickCounter).
8. `AutoDepositManager`: `ClientPlayerInteractionManager.interactBlock/clickSlot`, `ActionResult.shouldSwingHand`,
   `GenericContainerScreen.getScreenHandler`, `Slot.inventory/getIndex/id`.
9. `Ui`/`MainScreen`: `DrawContext.drawItem/enableScissor/drawBorder/drawText`, `MatrixStack.translate(float,float,float)`,
   `Screen.mouseScrolled(4 argumenty)`, `KeybindsScreen(Screen, GameOptions)`.
10. `MiningController`: pole `Entity.horizontalCollision`, `Vec3d.fromPolar`, `RaycastContext(…, Entity)`.
11. `ClientPlayConnectionEvents.DISCONNECT`, `ClientLifecycleEvents.CLIENT_STARTED/CLIENT_STOPPING`.

## Znane ograniczenia
- Kod niekompilowany i nietestowany w grze (patrz wyżej).
- Używane są tylko kilofy z paska szybkiego dostępu (brak podmiany z głównego ekwipunku).
- Bloki "customowe" serwerów, które współdzielą ID z vanillą (np. inna nazwa wyświetlana), są nierozróżnialne;
  rozpoznawanie jest po ID.
- Uczenie dropów (`autoLearnDrops`) opiera się na przyroście przedmiotów w 40 ticków po potwierdzonym wykopaniu
  zaznaczonego bloku; może nauczyć się przypadkowo podniesionego przedmiotu (pomijane są przedmioty o stosie 1).
- Liczniki przedmiotów liczą każdy przyrost w ekwipunku, gdy Auto Miner działa (także podniesione z ziemi inne itemy).
- Potwierdzenie wykopania po 6 tickach; przy dużym laga może dojść do błędnego zaliczenia lub odrzucenia.
- Deposit: skrzynia musi być w zasięgu (brak automatycznego dojścia do skrzyni); obsługa skrzyń, dużych skrzyń i beczek
  (ekran GenericContainer); shulker box i ender chest nie są obsługiwane.
- "Walk to next block" jest eksperymentalne i domyślnie wyłączone (prosta kontrola lawy/przepaści).
- Auto Miner obraca kamerę z ustawioną prędkością (domyślnie 20°/tick); niektóre antycheaty mogą to wykrywać.
  Mod nie próbuje unikać wykrycia; wiele serwerów zabrania automatycznego kopania.
- F5: przy pierwszym starcie vanilla "Przełącz widok" jest przenoszone na F4 (opcja).
- GUI: tylko angielskie etykiety, bez wklejania ze schowka w polach tekstowych, bez podpowiedzi (tooltipów);
  układ nie był sprawdzany przy różnych skalach GUI.
- Brak gradle wrappera (workflow instaluje Gradle 8.10.2 przez setup-gradle).

## Zasady projektu (z preferencji użytkownika)
- Kod i pliki są źródłem prawdy, nie czat.
- Żadnych usunięć plików bez potwierdzenia; żadnych haseł/tokenów w dokumentach.
- Po każdym etapie: co zapisano, jakie pliki, czy testy przeszły, następny krok.
