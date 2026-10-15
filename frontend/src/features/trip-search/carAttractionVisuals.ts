/**
 * Dokładne grafiki wybranych atrakcji z wyszukiwania samochodowego.
 * Lokalnie zapisane kadry to ilustracje AI, NIE dokumentalne fotografie.
 * Inne miejsca: fotografia Commons z atrybucją autora i licencji,
 * z wizualizacją AI jako wyraźnie oznaczonym fallbackiem.
 * Dopasowanie po pełnej nazwie destynacji, więc kilka atrakcji jednego miasta
 * nie otrzyma automatycznie tego samego obrazu.
 */
export type CarVisual = {
  destination: string;
  landmark: string;
  local?: string;
  commonsQuery?: string;
  fallback?: string;
};

export function normalizeCarDestination(value: string): string {
  return value.toLowerCase().replace(/ł/g, 'l').replace(/ß/g, 'ss')
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, ' ').trim();
}

const VISUALS: Record<string, CarVisual> = {
  "Oleśnica — Zamek Książęcy": { destination: "Oleśnica — Zamek Książęcy", landmark: "Zamek Książęcy", local: "/destinations/car/olesnica-zamek-ksiazecy.webp" },
  "Dzierżoniów — Arboretum Wojsławice": { destination: "Dzierżoniów — Arboretum Wojsławice", landmark: "Arboretum Wojsławice", local: "/destinations/car/dzierzoniow-arboretum-wojslawice.webp" },
  "Świdnica — Kościół Pokoju": { destination: "Świdnica — Kościół Pokoju", landmark: "Kościół Pokoju", local: "/destinations/car/swidnica-kosciol-pokoju.webp" },
  "Wałbrzych — Zamek Książ": { destination: "Wałbrzych — Zamek Książ", landmark: "Zamek Książ", local: "/destinations/car/walbrzych-zamek-ksiaz.webp" },
  "Ząbkowice Śląskie — Krzywa Wieża": { destination: "Ząbkowice Śląskie — Krzywa Wieża", landmark: "Krzywa Wieża", local: "/destinations/car/zabkowice-slaskie-krzywa-wieza.webp" },
  "Legnica — Zamek Piastowski": { destination: "Legnica — Zamek Piastowski", landmark: "Zamek Piastowski", local: "/destinations/car/legnica-zamek-piastowski.webp" },
  "Bolków — Zamek Bolków": { destination: "Bolków — Zamek Bolków", landmark: "Zamek Bolków", local: "/destinations/car/bolkow-zamek-bolkow.webp" },
  "Kłodzko — Twierdza Kłodzko": { destination: "Kłodzko — Twierdza Kłodzko", landmark: "Twierdza Kłodzko", local: "/destinations/car/klodzko-twierdza-klodzko.webp" },
  "Opole — Wieża Piastowska": { destination: "Opole — Wieża Piastowska", landmark: "Wieża Piastowska", local: "/destinations/car/opole-wieza-piastowska.webp" },
  "Adršpach — Skalne Miasto": { destination: "Adršpach — Skalne Miasto", landmark: "Skalne Miasto", local: "/destinations/car/adrspach-skalne-miasto.webp" },
  "Radków — Szczeliniec Wielki": { destination: "Radków — Szczeliniec Wielki", landmark: "Szczeliniec Wielki", local: "/destinations/car/radkow-szczeliniec-wielki.webp" },
  "Teplice nad Metují — Teplickie Skały": { destination: "Teplice nad Metují — Teplickie Skały", landmark: "Teplickie Skały", local: "/destinations/car/teplice-nad-metuji-teplickie-skaly.webp" },
  "Polanica-Zdrój — Park Zdrojowy": { destination: "Polanica-Zdrój — Park Zdrojowy", landmark: "Park Zdrojowy", local: "/destinations/car/polanica-zdroj-park-zdrojowy.webp" },
  "Kudowa-Zdrój — Błędne Skały": { destination: "Kudowa-Zdrój — Błędne Skały", landmark: "Błędne Skały", local: "/destinations/car/kudowa-zdroj-bledne-skaly.webp" },
  "Moszna — Zamek Moszna": { destination: "Moszna — Zamek Moszna", landmark: "Zamek Moszna", local: "/destinations/car/moszna-zamek-moszna.webp" },
  "Kletno — Jaskinia Niedźwiedzia": { destination: "Kletno — Jaskinia Niedźwiedzia", landmark: "Jaskinia Niedźwiedzia", local: "/destinations/car/kletno-jaskinia-niedzwiedzia.webp" },
  "Karpacz — Świątynia Wang": { destination: "Karpacz — Świątynia Wang", landmark: "Świątynia Wang", local: "/destinations/car/karpacz-swiatynia-wang.webp" },
  "Pec pod Sněžkou — Śnieżka od strony Czech": { destination: "Pec pod Sněžkou — Śnieżka od strony Czech", landmark: "Śnieżka od strony Czech", local: "/destinations/car/pec-pod-snezkou-sniezka-od-strony-czech.webp" },
  "Międzygórze — Wodospad Wilczki": { destination: "Międzygórze — Wodospad Wilczki", landmark: "Wodospad Wilczki", local: "/destinations/car/miedzygorze-wodospad-wilczki.webp" },
  "Karpacz — Śnieżka": { destination: "Karpacz — Śnieżka", landmark: "Śnieżka", local: "/destinations/car/karpacz-sniezka.webp" },
  "Jelenia Góra — Zamek Chojnik": { destination: "Jelenia Góra — Zamek Chojnik", landmark: "Zamek Chojnik", local: "/destinations/car/jelenia-gora-zamek-chojnik.webp" },
  "Szklarska Poręba — Wodospad Szklarki": { destination: "Szklarska Poręba — Wodospad Szklarki", landmark: "Wodospad Szklarki", local: "/destinations/car/szklarska-poreba-wodospad-szklarki.webp" },
  "Szklarska Poręba — Wodospad Kamieńczyka": { destination: "Szklarska Poręba — Wodospad Kamieńczyka", landmark: "Wodospad Kamieńczyka", local: "/destinations/car/szklarska-poreba-wodospad-kamienczyka.webp" },
  "Harrachov — Wodospad Mumlawy": { destination: "Harrachov — Wodospad Mumlawy", landmark: "Wodospad Mumlawy", local: "/destinations/car/harrachov-wodospad-mumlawy.webp" },
  "Zielona Góra — Palmiarnia": { destination: "Zielona Góra — Palmiarnia", landmark: "Palmiarnia", local: "/destinations/car/zielona-gora-palmiarnia.webp" },
  "Jičín — Prachowskie Skały": { destination: "Jičín — Prachowskie Skały", landmark: "Prachowskie Skały", local: "/destinations/car/jicin-prachowskie-skaly.webp" },
  "Görlitz — Stare Miasto": { destination: "Görlitz — Stare Miasto", landmark: "Stare Miasto", local: "/destinations/car/gorlitz-stare-miasto.webp" },
  "Poznań — Stary Rynek": { destination: "Poznań — Stary Rynek", landmark: "Stary Rynek", commonsQuery: "Poznan Old Market Square Town Hall Poland", fallback: "/destinations/poznan.webp" },
  "Litomyšl — Zamek Litomyšl": { destination: "Litomyšl — Zamek Litomyšl", landmark: "Zamek Litomyšl", commonsQuery: "Litomysl castle Renaissance UNESCO Czechia", fallback: "/destinations/car/olesnica-zamek-ksiazecy.webp" },
  "Częstochowa — Jasna Góra": { destination: "Częstochowa — Jasna Góra", landmark: "Jasna Góra", commonsQuery: "Jasna Gora monastery Czestochowa Poland", fallback: "/destinations/car/swidnica-kosciol-pokoju.webp" },
  "Pardubice — Zamek Pardubice": { destination: "Pardubice — Zamek Pardubice", landmark: "Zamek Pardubice", commonsQuery: "Pardubice castle Czechia", fallback: "/destinations/car/legnica-zamek-piastowski.webp" },
  "Liberec — Ještěd": { destination: "Liberec — Ještěd", landmark: "Ještěd", commonsQuery: "Jested tower Liberec Czechia", fallback: "/destinations/car/karpacz-sniezka.webp" },
  "Olsztyn koło Częstochowy — Zamek Olsztyn": { destination: "Olsztyn koło Częstochowy — Zamek Olsztyn", landmark: "Zamek Olsztyn", commonsQuery: "Olsztyn castle ruins near Czestochowa Poland", fallback: "/destinations/car/bolkow-zamek-bolkow.webp" },
  "Gniezno — Katedra": { destination: "Gniezno — Katedra", landmark: "Katedra", commonsQuery: "Gniezno cathedral Poland", fallback: "/destinations/car/swidnica-kosciol-pokoju.webp" },
  "Świebodzin — Figura Chrystusa Króla": { destination: "Świebodzin — Figura Chrystusa Króla", landmark: "Figura Chrystusa Króla", commonsQuery: "Christ the King statue Swiebodzin Poland", fallback: "/destinations/swiebodzin.webp" },
  "Ostrawa — Dolne Vitkovice": { destination: "Ostrawa — Dolne Vitkovice", landmark: "Dolne Vitkovice", commonsQuery: "Dolni Vitkovice Ostrava blast furnaces Czechia", fallback: "/destinations/car/zielona-gora-palmiarnia.webp" },
  "Ołomuniec — Kolumna Trójcy Świętej": { destination: "Ołomuniec — Kolumna Trójcy Świętej", landmark: "Kolumna Trójcy Świętej", commonsQuery: "Holy Trinity Column Olomouc Czechia", fallback: "/destinations/car/opole-wieza-piastowska.webp" },
  "Kutná Hora — Kaplica Czaszek": { destination: "Kutná Hora — Kaplica Czaszek", landmark: "Kaplica Czaszek", commonsQuery: "Sedlec Ossuary Kutna Hora Czechia", fallback: "/destinations/car/kletno-jaskinia-niedzwiedzia.webp" },
  "Kutná Hora — Katedra św. Barbary": { destination: "Kutná Hora — Katedra św. Barbary", landmark: "Katedra św. Barbary", commonsQuery: "Saint Barbara Church Kutna Hora Czechia", fallback: "/destinations/car/swidnica-kosciol-pokoju.webp" },
  "Budziszyn — Stare Miasto": { destination: "Budziszyn — Stare Miasto", landmark: "Stare Miasto", commonsQuery: "Bautzen old town skyline Germany", fallback: "/destinations/car/gorlitz-stare-miasto.webp" },
  "Łódź — Ulica Piotrkowska": { destination: "Łódź — Ulica Piotrkowska", landmark: "Ulica Piotrkowska", commonsQuery: "Piotrkowska Street Lodz Poland", fallback: "/destinations/lodz.webp" },
  "Oświęcim — Auschwitz-Birkenau": { destination: "Oświęcim — Auschwitz-Birkenau", landmark: "Auschwitz-Birkenau", commonsQuery: "Auschwitz Birkenau memorial Oswiecim Poland", fallback: "/destinations/car/klodzko-twierdza-klodzko.webp" },
  "Děčín — Pravčická Brána": { destination: "Děčín — Pravčická Brána", landmark: "Pravčická Brána", commonsQuery: "Pravcicka brana Czech Switzerland sandstone arch", fallback: "/destinations/car/jicin-prachowskie-skaly.webp" },
  "Morawski Kras — Przepaść Macocha": { destination: "Morawski Kras — Przepaść Macocha", landmark: "Przepaść Macocha", commonsQuery: "Macocha Abyss Moravian Karst Czechia", fallback: "/destinations/car/kletno-jaskinia-niedzwiedzia.webp" },
  "Hřensko — Wąwóz Edmunda": { destination: "Hřensko — Wąwóz Edmunda", landmark: "Wąwóz Edmunda", commonsQuery: "Edmund Gorge Hrensko Czechia", fallback: "/destinations/car/teplice-nad-metuji-teplickie-skaly.webp" },
  "Kromieryż — Ogrody Kwiatowe": { destination: "Kromieryż — Ogrody Kwiatowe", landmark: "Ogrody Kwiatowe", commonsQuery: "Kromeriz Flower Garden Czechia", fallback: "/destinations/car/dzierzoniow-arboretum-wojslawice.webp" },
  "Zator — Energylandia": { destination: "Zator — Energylandia", landmark: "Energylandia", commonsQuery: "Energylandia amusement park Zator Poland", fallback: "/destinations/car/dzierzoniow-arboretum-wojslawice.webp" },
  "Saksonia Szwajcarska — Bastei": { destination: "Saksonia Szwajcarska — Bastei", landmark: "Bastei", commonsQuery: "Bastei bridge Saxon Switzerland Germany", fallback: "/destinations/car/radkow-szczeliniec-wielki.webp" },
  "Bad Schandau — Twierdza Königstein": { destination: "Bad Schandau — Twierdza Königstein", landmark: "Twierdza Königstein", commonsQuery: "Festung Konigstein Saxony Germany", fallback: "/destinations/car/klodzko-twierdza-klodzko.webp" },
  "Brno — Willa Tugendhat": { destination: "Brno — Willa Tugendhat", landmark: "Willa Tugendhat", commonsQuery: "Villa Tugendhat Brno Czechia", fallback: "/destinations/car/legnica-zamek-piastowski.webp" },
  "Brno — Zamek Špilberk": { destination: "Brno — Zamek Špilberk", landmark: "Zamek Špilberk", commonsQuery: "Spilberk Castle Brno Czechia", fallback: "/destinations/car/klodzko-twierdza-klodzko.webp" },
  "Ojców — Zamek w Pieskowej Skale": { destination: "Ojców — Zamek w Pieskowej Skale", landmark: "Zamek w Pieskowej Skale", commonsQuery: "Pieskowa Skala Castle Ojcow Poland", fallback: "/destinations/car/walbrzych-zamek-ksiaz.webp" },
  "Praga — Most Karola": { destination: "Praga — Most Karola", landmark: "Most Karola", commonsQuery: "Charles Bridge Prague Czechia", fallback: "/destinations/praga.webp" },
  "Praga — Zamek na Hradczanach": { destination: "Praga — Zamek na Hradczanach", landmark: "Zamek na Hradczanach", commonsQuery: "Prague Castle Hradcany Czechia", fallback: "/destinations/praga.webp" },
  "Třebíč — Dzielnica Żydowska": { destination: "Třebíč — Dzielnica Żydowska", landmark: "Dzielnica Żydowska", commonsQuery: "Trebic Jewish Quarter Czechia", fallback: "/destinations/car/gorlitz-stare-miasto.webp" },
  "Drezno — Frauenkirche": { destination: "Drezno — Frauenkirche", landmark: "Frauenkirche", commonsQuery: "Frauenkirche Dresden Germany", fallback: "/destinations/drezno.webp" },
  "Drezno — Zwinger": { destination: "Drezno — Zwinger", landmark: "Zwinger", commonsQuery: "Zwinger Palace Dresden Germany", fallback: "/destinations/drezno.webp" },
  "Bydgoszcz — Wyspa Młyńska": { destination: "Bydgoszcz — Wyspa Młyńska", landmark: "Wyspa Młyńska", commonsQuery: "Mill Island Bydgoszcz Poland", fallback: "/destinations/car/zielona-gora-palmiarnia.webp" },
  "Kraków — Rynek Główny": { destination: "Kraków — Rynek Główny", landmark: "Rynek Główny", commonsQuery: "Main Market Square Krakow Poland cloth hall", fallback: "/destinations/krakow.webp" },
  "Kraków — Wawel": { destination: "Kraków — Wawel", landmark: "Wawel", commonsQuery: "Wawel Castle Krakow Poland", fallback: "/destinations/krakow.webp" },
  "Toruń — Stare Miasto": { destination: "Toruń — Stare Miasto", landmark: "Stare Miasto", commonsQuery: "Torun Old Town Town Hall Poland", fallback: "/destinations/car/gorlitz-stare-miasto.webp" },
  "Telč — Renesansowy Rynek": { destination: "Telč — Renesansowy Rynek", landmark: "Renesansowy Rynek", commonsQuery: "Telc Renaissance Square Czechia", fallback: "/destinations/car/gorlitz-stare-miasto.webp" },
  "Wieliczka — Kopalnia Soli": { destination: "Wieliczka — Kopalnia Soli", landmark: "Kopalnia Soli", commonsQuery: "Wieliczka Salt Mine St Kinga Chapel Poland", fallback: "/destinations/car/kletno-jaskinia-niedzwiedzia.webp" },
  "Miśnia — Albrechtsburg": { destination: "Miśnia — Albrechtsburg", landmark: "Albrechtsburg", commonsQuery: "Albrechtsburg Castle Meissen Germany", fallback: "/destinations/car/walbrzych-zamek-ksiaz.webp" },
  "Mikulov — Zamek Mikulov": { destination: "Mikulov — Zamek Mikulov", landmark: "Zamek Mikulov", commonsQuery: "Mikulov Castle Czechia", fallback: "/destinations/car/olesnica-zamek-ksiazecy.webp" },
  "Zakopane — Gubałówka": { destination: "Zakopane — Gubałówka", landmark: "Gubałówka", commonsQuery: "Gubalowka Zakopane panorama Tatras Poland", fallback: "/destinations/car/karpacz-sniezka.webp" },
  "Kościelisko — Dolina Kościeliska": { destination: "Kościelisko — Dolina Kościeliska", landmark: "Dolina Kościeliska", commonsQuery: "Koscieliska Valley Tatras Poland", fallback: "/destinations/car/radkow-szczeliniec-wielki.webp" },
  "Białka Tatrzańska — Termy Bania": { destination: "Białka Tatrzańska — Termy Bania", landmark: "Termy Bania", commonsQuery: "Terma Bania Bialka Tatrzanska thermal pools Poland", fallback: "/destinations/car/polanica-zdroj-park-zdrojowy.webp" },
  "Warszawa — Łazienki Królewskie": { destination: "Warszawa — Łazienki Królewskie", landmark: "Łazienki Królewskie", commonsQuery: "Lazienki Park Palace on Isle Warsaw Poland", fallback: "/destinations/car/polanica-zdroj-park-zdrojowy.webp" },
  "Warszawa — Stare Miasto": { destination: "Warszawa — Stare Miasto", landmark: "Stare Miasto", commonsQuery: "Warsaw Old Town Market Square Poland", fallback: "/destinations/warszawa.webp" },
  "Zakopane — Morskie Oko": { destination: "Zakopane — Morskie Oko", landmark: "Morskie Oko", commonsQuery: "Morskie Oko Lake Tatras Poland", fallback: "/destinations/car/adrspach-skalne-miasto.webp" },
  "Szczecin — Wały Chrobrego": { destination: "Szczecin — Wały Chrobrego", landmark: "Wały Chrobrego", commonsQuery: "Szczecin Waly Chrobrego promenade Poland", fallback: "/destinations/szczecin.webp" },
};

const NORMALIZED_VISUALS = new Map<string, CarVisual>(
  Object.entries(VISUALS).map(([key, visual]) => [normalizeCarDestination(key), visual]),
);

/** Używaj pełnej nazwy, np. „Drezno — Zwinger”, nie samego miasta. */
export function getCarVisual(destination: string): CarVisual | undefined {
  return NORMALIZED_VISUALS.get(normalizeCarDestination(destination));
}

export const CAR_VISUAL_DESTINATION_COUNT = Object.keys(VISUALS).length;
