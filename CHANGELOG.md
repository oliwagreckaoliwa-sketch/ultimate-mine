# CHANGELOG

## 2026-11-22 — 0.1.0 (szkielet)
- Dodano settings.gradle, build.gradle (Fabric Loom), gradle.properties.
- Dodano fabric.mod.json (environment: client, Java 21, MC ~1.21.1).
- Dodano workflow GitHub Actions do budowania JAR.
- Dodano PROJECT_STATE.md, TODO.md, CHANGELOG.md, DECISIONS.md.
- Kompilacja niemożliwa w tej sesji (403 na hostach Maven/Mojang/Gradle).

## 2026-11-22 — etapy 1, 2, 4 (kod, niezkompilowany)
- config: ModConfig, ConfigManager (atomowy zapis, kopia przy uszkodzonym pliku).
- selector: BlockGroup, BlockInfo, BlockSelectionManager (grupy, zapis/odczyt list), BlockSelector (F5, obrys).
- notifications: NotificationManager.
- statistics: ResourceCounter, SessionStatistics, StatisticsManager (historia sesji, zapis), InventoryWatcher.
- mining: BlockWhitelist, ToolManager, MiningTargetSelector, MiningController, AutoMiningManager.
- util: Fmt, AimHelper. gui: Draw (prymitywy).

## 2026-11-22 — etap 3 (deposit), keybindy, HUD, AutoMinerClient (kod, niezkompilowany)
- deposit: ChestManager, InventoryTransfer, AutoDepositManager (automat stanow, potwierdzenie przez porownanie ekwipunku).
- keybinds: KeybindManager (F5..F9 + 3 nieprzypisane: pauza, wybor skrzynki, deposit now; remap vanilla F5 -> F4).
- hud: AutoMinerHud (model), HudRenderer (HUD, podpis bloku pod celownikiem, powiadomienia).
- AutoMinerClient: spiecie modulow, zapis przy zamknieciu, obsluga rozlaczenia.
- Maven nadal blokowany (403) — ponowna kontrola po przerwie.

## 2026-11-22 — GUI, zasoby, dokumentacja (kod niezkompilowany)
- gui: Ui (immediate-mode: przyciski, przełączniki, suwaki, pola tekstowe, scroll, animacje), GuiPanel, MainScreen,
  DashboardScreen, MiningScreen, BlockSelectorScreen, DepositScreen, StatisticsScreen, HudSettingsScreen,
  SettingsScreen, HudEditScreen.
- resources: lang/en_us.json, lang/pl_pl.json, icons/icon.png; fabric.mod.json: ikona.
- README.md (kompilacja GitHub, instalacja, skróty, ograniczenia), LICENSE (MIT), .gitignore.
- Workflow: Gradle 8.10.2 przez setup-gradle, artefakt JAR bez -sources, raporty przy błędzie.
- Kontrola składni javac: 0 błędów parsowania; spójność odwołań między własnymi klasami OK.
