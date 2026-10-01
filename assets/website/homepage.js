(() => {
  const root = document.documentElement;
  const themeButton = document.querySelector('.theme-toggle');
  const themeColor = document.querySelector('meta[name="theme-color"]');
  const systemTheme = matchMedia('(prefers-color-scheme: dark)');
  let manualTheme = false;
  try { manualTheme = ['light', 'dark'].includes(localStorage.getItem('purely-home-theme')); } catch (_) {}
  function updateTheme(theme) {
    root.dataset.theme = theme;
    const isDark = theme === 'dark';
    const label = isDark ? '切换到浅色模式' : '切换到深色模式';
    themeButton.setAttribute('aria-label', label);
    themeButton.title = label;
    themeButton.firstElementChild.className = `icon ${isDark ? 'i-sun' : 'i-moon'}`;
    themeColor.content = isDark ? '#171a15' : '#f1f2ee';
  }
  updateTheme(root.dataset.theme);
  themeButton.addEventListener('click', () => {
    const theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
    manualTheme = true;
    try { localStorage.setItem('purely-home-theme', theme); } catch (_) {}
    updateTheme(theme);
  });
  systemTheme.addEventListener('change', (event) => { if (!manualTheme) updateTheme(event.matches ? 'dark' : 'light'); });

  const menuButton = document.querySelector('.menu-toggle');
  const navigation = document.getElementById('primary-nav');
  function setMenu(open) {
    navigation.classList.toggle('is-open', open);
    menuButton.setAttribute('aria-expanded', String(open));
    menuButton.setAttribute('aria-label', open ? '收起导航' : '展开导航');
    menuButton.firstElementChild.className = `icon ${open ? 'i-x' : 'i-menu'}`;
  }
  menuButton.addEventListener('click', () => setMenu(menuButton.getAttribute('aria-expanded') !== 'true'));
  navigation.addEventListener('click', (event) => { if (event.target.closest('a')) setMenu(false); });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && menuButton.getAttribute('aria-expanded') === 'true') { setMenu(false); menuButton.focus(); }
  });
  matchMedia('(min-width: 768px)').addEventListener('change', () => setMenu(false));

  const lyricButton = document.getElementById('lyric-toggle');
  const lyricSample = document.getElementById('lyric-sample');
  lyricButton.hidden = false;
  lyricButton.addEventListener('click', () => {
    const isSingle = lyricSample.dataset.style !== 'single';
    lyricSample.dataset.style = isSingle ? 'single' : 'multi';
    lyricButton.textContent = isSingle ? '切回多行样式' : '试试单行样式';
  });

  const albumShelf = document.querySelector('.album-shelf');
  const galleryButtons = document.querySelector('.gallery-buttons');
  const galleryCaption = document.getElementById('gallery-caption');
  galleryButtons.hidden = false;
  galleryButtons.addEventListener('click', (event) => {
    if (!event.target.closest('button')) return;
    const showCoast = albumShelf.dataset.active === 'red';
    albumShelf.dataset.active = showCoast ? 'coast' : 'red';
    galleryCaption.textContent = showCoast ? '内置封面 / 海岸日落' : '内置封面 / 红色纹理';
  });

  // All content is visible without JavaScript, reduced motion or observer support.
  const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)');
  const revealElements = [...document.querySelectorAll('.reveal')];
  if ('IntersectionObserver' in window && !reducedMotion.matches) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => { if (entry.isIntersecting) { entry.target.classList.remove('is-pending'); observer.unobserve(entry.target); } });
    }, { threshold: 0.08 });
    revealElements.forEach((element) => {
      if (element.getBoundingClientRect().top > window.innerHeight) { element.classList.add('is-pending'); observer.observe(element); }
    });
    const showAll = () => { revealElements.forEach((element) => element.classList.remove('is-pending')); observer.disconnect(); };
    reducedMotion.addEventListener('change', showAll, { once: true });
    window.addEventListener('beforeprint', showAll);
  }

  // Motion is progressive enhancement. Static content and controls stay usable
  // if the local animation libraries are unavailable or motion is reduced.
  if (window.gsap && window.ScrollTrigger) {
    const { gsap, ScrollTrigger } = window;
    gsap.registerPlugin(ScrollTrigger);
    const motion = gsap.matchMedia();
    motion.add('screen and (prefers-reduced-motion: no-preference)', () => {
      gsap.timeline({ defaults: { duration: 1, ease: 'power3.out' } })
        .from('.hero-copy > .eyebrow, .hero-word, .hero-title-cn, .hero-description, .hero-actions', {
          y: 28, opacity: 0, stagger: .09, clearProps: 'transform,opacity'
        })
        .from('.record-stage', { y: 42, rotation: -4, opacity: 0, duration: 1.4, clearProps: 'transform,opacity' }, .12);

      const marqueeToggle = document.querySelector('.marquee-toggle');
      const marquee = document.querySelector('.format-marquee');
      root.classList.add('has-marquee');
      marqueeToggle.hidden = false;
      let manuallyPaused = false;
      let inView = false;
      let pointerInside = false;
      const ticker = gsap.to('.format-track', { xPercent: -50, duration: 42, repeat: -1, ease: 'none', paused: true });
      const updateTicker = () => { ticker.paused(manuallyPaused || !inView || pointerInside || document.hidden); };
      const toggleTicker = () => {
        manuallyPaused = !manuallyPaused;
        marqueeToggle.setAttribute('aria-pressed', String(manuallyPaused));
        marqueeToggle.textContent = manuallyPaused ? '继续滚动' : '暂停滚动';
        updateTicker();
      };
      const enterMarquee = () => { pointerInside = true; updateTicker(); };
      const leaveMarquee = () => { pointerInside = false; updateTicker(); };
      marqueeToggle.addEventListener('click', toggleTicker);
      marquee.addEventListener('pointerenter', enterMarquee);
      marquee.addEventListener('pointerleave', leaveMarquee);
      document.addEventListener('visibilitychange', updateTicker);
      let tickerObserver;
      if ('IntersectionObserver' in window) {
        tickerObserver = new IntersectionObserver(([entry]) => { inView = entry.isIntersecting; updateTicker(); });
        tickerObserver.observe(marquee);
      } else { inView = true; updateTicker(); }

      const wordGroups = [...document.querySelectorAll('.scrub-words')];
      wordGroups.forEach((group) => {
        const characters = [...group.textContent].map((character) => {
          const span = document.createElement('span');
          span.className = 'scrub-character';
          span.textContent = character;
          return span;
        });
        group.replaceChildren(...characters);
        gsap.fromTo(characters, { opacity: .72 }, {
          opacity: 1, stagger: .1, ease: 'none',
          scrollTrigger: { trigger: group.closest('.story-line'), start: 'top 78%', end: 'top 44%', scrub: .5 }
        });
      });

      return () => {
        tickerObserver?.disconnect();
        marqueeToggle.removeEventListener('click', toggleTicker);
        marquee.removeEventListener('pointerenter', enterMarquee);
        marquee.removeEventListener('pointerleave', leaveMarquee);
        document.removeEventListener('visibilitychange', updateTicker);
        root.classList.remove('has-marquee');
        marqueeToggle.hidden = true;
        marqueeToggle.setAttribute('aria-pressed', 'false');
        marqueeToggle.textContent = '暂停滚动';
        wordGroups.forEach((group) => { group.textContent = group.textContent; });
      };
    });
    motion.add('screen and (min-width: 1024px) and (prefers-reduced-motion: no-preference)', () => {
      ScrollTrigger.create({
        trigger: '.story-copy', pin: true, pinSpacing: false,
        start: 'top 130px', endTrigger: '.story-lines', end: 'bottom 55%',
        invalidateOnRefresh: true
      });
    });
    document.fonts.ready.then(() => ScrollTrigger.refresh());
    window.addEventListener('load', () => ScrollTrigger.refresh(), { once: true });
  }
  document.getElementById('year').textContent = String(new Date().getFullYear());
  document.querySelectorAll('a[href="#faq-network"]').forEach((link) => {
    link.addEventListener('click', () => { document.getElementById('faq-network').open = true; });
  });
})();
