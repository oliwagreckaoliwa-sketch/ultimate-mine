# AutoMiner Ultimate

Kliencki mod Fabric dla **Minecraft Java 1.21.1** (Java 21): Auto Miner, Block Selector (F5),
Auto Deposit do skrzyni, statystyki z historią sesji, HUD i ciemne GUI (F9).

> **Status: kod NIE został jeszcze skompilowany ani uruchomiony w grze.** Środowisko, w którym powstał,
> nie miało dostępu do Maven Fabric/Mojang. Pierwsza kompilacja (GitHub Actions) może wymagać drobnych poprawek
> nazw metod Yarn. Lista miejsc ryzykownych: `PROJECT_STATE.md` → "Niezweryfikowane API".

## Kompilacja przez GitHub Actions

1. Utwórz repozytorium na GitHubie i wypchnij cały katalog projektu (z `.github/workflows/build.yml`).
2. Wejdź w zakładkę **Actions** → workflow **build** (uruchamia się po każdym pushu; można też **Run workflow**).
3. Po zielonym przebiegu pobierz artefakt **autominer-ultimate-jar** → plik `autominer-ultimate-0.1.0.jar`.
4. Przy czerwonym przebiegu otwórz log kroku **Build**, skopiuj błędy `error:` (nazwa pliku, linia, komunikat)
   i prześlij je do poprawki.

## Kompilacja lokalna

Wymagane: JDK 21 i Gradle 8.10+ (albo wygeneruj wrapper: `gradle wrapper --gradle-version 8.10.2`).

```
gradle build
```
Gotowy JAR: `build/libs/autominer-ultimate-0.1.0.jar` (pliku `-sources.jar` nie instaluj).

## Instalacja moda

1. Zainstaluj **Fabric Loader** dla Minecraft 1.21.1 (https://fabricmc.net/use/installer/).
2. Pobierz **Fabric API** dla 1.21.1 i wrzuć do `.minecraft/mods`.
3. Wrzuć `autominer-ultimate-0.1.0.jar` do `.minecraft/mods`.
4. Uruchom grę profilem Fabric (Java 21).

Konfiguracja: `.minecraft/config/autominer-ultimate/config.json`, statystyki: `stats.json`, listy bloków: `lists/*.json`.

## Skróty (Opcje → Sterowanie → AutoMiner Ultimate)

| Klawisz | Akcja |
|---|---|
| F5 | dodaj wskazywany blok do aktywnej grupy |
| Shift + F5 | usuń wskazywany rodzaj bloku ze wszystkich grup |
| F6 | włącz / wyłącz Auto Miner |
| F7 | włącz / wyłącz Auto Deposit |
| F8 | pokaż / ukryj HUD |
| F9 | główne menu |
| (nieprzypisane) | pauza Auto Miner, wybór skrzyni, depozyt teraz |

**Uwaga o F5:** vanilla używa F5 do zmiany widoku kamery, a Minecraft pozwala jednemu klawiszowi obsługiwać
tylko jedną akcję. Przy pierwszym starcie mod przenosi vanilla "Przełącz widok" na **F4**
(wyłączalne w General Settings; wtedy ręcznie rozwiąż konflikt w Sterowaniu).

## Jak to działa

- **Block Selector**: zaznaczasz bloki celownikiem; zapis po identyfikatorach (`minecraft:stone`,
  `mod:custom_rock`), grupy, zapis/wczytanie list. Obrys: zielony = zaznaczony, szary = nie.
- **Auto Miner**: celuje kamerą w najbliższy zaznaczony widoczny blok i "trzyma" klawisz ataku (zwykła operacja
  klienta), wybiera najlepszy kilof z paska szybkiego dostępu, zatrzymuje się przy zbyt zniszczonym kilofie,
  braku kilofa, obrażeniach, odrzuceniu kopania przez serwer.
- **Auto Deposit**: podchodzisz w zasięg zapisanej skrzyni; mod celuje w nią, otwiera ją zwykłym kliknięciem
  i przenosi pasujące przedmioty shift-kliknięciem. Przenoszenie jest potwierdzane porównaniem ekwipunku;
  przedmioty nie są wyrzucane ani usuwane, a przy pełnej skrzyni zostają u gracza.
- **Statystyki**: liczniki bloków (po potwierdzeniu przez serwer) i przedmiotów (z przyrostu w ekwipunku)
  są rozdzielone; zapis do JSON, historia sesji, reset.

## Znane ograniczenia

Pełna lista w `PROJECT_STATE.md` (sekcja "Znane ograniczenia"). Najważniejsze:
mod nie omija antycheatów, więc na części serwerów automatyczne kopanie lub otwieranie skrzyń może być
zabronione (zasady serwera!). Używaj tylko tam, gdzie regulamin na to pozwala.
