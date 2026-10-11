import { Icon } from '../components/Icon';
import { TripSearchPage } from '../features/trip-search/TripSearchPage';

export default function App() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="container nav-content">
          <a href="#gora" className="brand" aria-label="Travel Finder — strona główna">
            <span className="brand-mark"><Icon name="compass" size={23} strokeWidth={2.1} /></span>
            <span>travel<span className="brand-light">finder</span><span className="brand-dot">.</span></span>
          </a>
          <nav className="main-nav" aria-label="Nawigacja główna">
            <a href="#wyszukiwarka" className="nav-link active">Odkrywaj</a>
            <a href="#jak-dziala" className="nav-link">Jak to działa</a>
          </nav>
          <div className="nav-aside">
            <span className="live-dot" />
            <span>Wersja demonstracyjna</span>
            <span className="version-tag">01</span>
          </div>
        </div>
      </header>

      <main id="gora">
        <section className="hero container" aria-labelledby="hero-title">
          <div className="hero-copy">
            <div className="eyebrow"><span className="eyebrow-line" /> ODKRYWAJ WIĘCEJ, PLANUJ MNIEJ</div>
            <h1 id="hero-title">Masz wolny weekend?<br /><em>Świat stoi otworem.</em></h1>
            <p>Ty wybierasz <strong>kiedy</strong> i <strong>za ile</strong>. My pokazujemy, <strong>dokąd warto pojechać</strong>. Bez wpisywania celu podróży.</p>
            <div className="hero-bullets">
              <span><Icon name="check" size={14} /> Z dowolnym kierunkiem</span>
              <span><Icon name="check" size={14} /> Wszystkie środki transportu</span>
            </div>
          </div>

          <div className="hero-art" aria-hidden="true">
            <div className="art-grid" />
            <div className="art-orbit orbit-outer" />
            <div className="art-orbit orbit-inner" />
            <div className="art-sun"><Icon name="sun" size={43} strokeWidth={1.2} /></div>
            <div className="orbit-dot orbit-dot-a" />
            <div className="orbit-dot orbit-dot-b" />
            <div className="art-pin pin-one"><Icon name="map-pin" size={15} /> Rzym <span>✦</span></div>
            <div className="art-pin pin-two"><Icon name="map-pin" size={15} /> Praga <span>↗</span></div>
            <div className="art-plane"><Icon name="plane" size={26} /></div>
            <div className="art-footer"><span>JEDEN TERMIN</span><span>↗</span><span>WIELE MOŻLIWOŚCI</span></div>
          </div>
        </section>

        <TripSearchPage />

        <section id="jak-dziala" className="how-section container">
          <div className="section-overline">PROSTO, TAK JAK LUBISZ</div>
          <div className="how-top"><h2>Od wolnego piątku do <em>nowego miejsca.</em></h2><p>Bez setek zakładek, porównywarek i ręcznego szukania kierunków.</p></div>
          <div className="how-grid">
            <article className="how-item"><span className="how-icon"><Icon name="calendar" size={22} /></span><span className="how-number">01 / TERMIN</span><h3>Wpisz, kiedy możesz</h3><p>Wybierz miasto, daty, godziny oraz budżet.</p></article>
            <article className="how-item"><span className="how-icon"><Icon name="route" size={22} /></span><span className="how-number">02 / MOŻLIWOŚCI</span><h3>Sprawdź różne kierunki</h3><p>Zobacz podróże samolotem, pociągiem, autobusem lub autem.</p></article>
            <article className="how-item"><span className="how-icon"><Icon name="sparkles" size={22} /></span><span className="how-number">03 / DECYZJA</span><h3>Wybierz swój weekend</h3><p>Porównaj czas na miejscu, godziny i cenę transportu.</p></article>
          </div>
        </section>
      </main>
      <footer className="site-footer container">
        <div className="footer-brand"><Icon name="compass" size={20} /> travelfinder<span>.</span></div>
        <p>Więcej wspomnień. Mniej planowania.</p>
        <span>© {new Date().getFullYear()} Travel Finder · MVP</span>
      </footer>
    </div>
  );
}
