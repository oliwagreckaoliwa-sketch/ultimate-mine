# TODO

## Następny krok
- [ ] Wypchnąć projekt na GitHub i uruchomić workflow "build"; przesłać błędy `error:` z kroku Build do poprawki.

## Po pierwszej udanej kompilacji
- [ ] Zweryfikować/uzupełnić wersje w gradle.properties i build.gradle (Loom, Loader, Fabric API, Yarn).
- [ ] Przejść listę "Niezweryfikowane API" z PROJECT_STATE.md.
- [ ] Testy ręczne w grze (spec. sekcja 12):
  1. start moda na Fabric 1.21.1
  2. dodawanie bloków celownikiem (F5)
  3. usuwanie (Shift+F5)
  4. bloki customowe / z innych modów
  5. automatyczne kopanie zaznaczonych bloków
  6. liczenie wykopanych surowców
  7. automatyczne depozytowanie
  8. pełna skrzynia
  9. zapis statystyk po restarcie
  10. wszystkie zakładki GUI (różne skale GUI)
  11. skróty klawiszowe
  12. Singleplayer
  13. Multiplayer z ograniczeniami serwera
- [ ] Wygenerować gradle wrapper (`gradle wrapper --gradle-version 8.10.2`) i dodać do repo.

## Pomysły / braki
- [ ] Podmiana kilofa z głównego ekwipunku na pasek.
- [ ] Automatyczne podejście do skrzyni (obecnie wymagany zasięg).
- [ ] Obsługa shulker boxów w depozycie.
- [ ] Polskie tłumaczenie etykiet GUI.
- [ ] Tooltips, wklejanie w polach tekstowych.
