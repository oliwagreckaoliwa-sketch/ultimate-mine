# DECISIONS

## D-001 — Mod tylko kliencki, bez własnych pakietów
Cała logika używa normalnych operacji klienta (`ClientPlayerInteractionManager`, kliknięcia slotów).
Brak własnych pakietów i ominięć antycheata; jeśli serwer odrzuci akcję, mod pokazuje komunikat
i nie liczy statystyk.

## D-002 — Wersje zależności niezweryfikowane
Yarn `1.21.1+build.3`, Loader `0.16.14`, Fabric API `0.116.7+1.21.1`, Loom `1.9-SNAPSHOT` wpisano z pamięci.
Brak dostępu do Maven w sesji, więc trzeba je potwierdzić przy pierwszym buildzie
(https://fabricmc.net/develop/). Mapowania: Yarn (nazwy klas w kodzie zgodne z Yarn 1.21.1).

## D-003 — Klawisz F5
F5 w vanilla przełącza widok kamery. Rejestrujemy własny keybind F5 (zgodnie ze specyfikacją);
konflikt jest widoczny w Controls i można go przypisać inaczej. Shift+F5 obsługiwany w handlerze keybinda.

## D-004 — Liczenie statystyk
Blok liczony dopiero po potwierdzeniu zmiany stanu na powietrze po stronie klienta (aktualizacja od serwera).
Przedmioty liczone po faktycznym wzroście ilości w ekwipunku, osobno od liczników bloków.

## D-005 — Dokumenty bez sekretów
Żadnych haseł, tokenów i kluczy API w plikach projektu.

## D-006 — Kompilacja przez GitHub Actions
Użytkownik wybrał opcję 1: kod jest pisany bez kompilatora w sesji, a JAR powstaje w GitHub Actions.
Błędy kompilacji wracają do poprawki. Lista ryzykownych API: PROJECT_STATE.md.

## D-007 — GUI w trybie immediate-mode
Własny lekki silnik (`Ui`) zamiast widgetów vanilla: pełna kontrola nad stylem i animacjami, mało stanu,
animacje to interpolacje z czasu klatki (bez obciążenia FPS).

## D-008 — Kopanie przez klawisz ataku
Auto Miner celuje kamerą i ustawia `attackKey` jako wciśnięty (zwykła ścieżka klienta). Kopie tylko zaznaczone bloki:
gdy celownik wskazuje inny, niezaznaczony blok, klawisz jest zwalniany. Brak własnych pakietów.

## D-009 — Dwa osobne zestawy liczników
`ResourceCounter` trzyma osobno bloki i przedmioty. Wiersze "Iron Ore/Gold Ore/Diamond Ore/Stone/Deepslate"
to liczniki bloków (rudy łączą wariant deepslate), "Raw Iron/Raw Gold/Diamonds/Cobblestone" to przedmioty.

## D-010 — Skrzynia w zasięgu
Auto Deposit nie chodzi do skrzyni; wymaga, by skrzynia była w zasięgu (zgodnie z zasadą "tylko normalne operacje").
