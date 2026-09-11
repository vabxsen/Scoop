const year = document.querySelector("#year");

if (year) {
  year.textContent = new Date().getFullYear().toString();
}

const demo = document.querySelector("#demo-video");
const demoToggle = document.querySelector("#demo-toggle");

if (demo && demoToggle) {
  let pausedByUser = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  const sync = () => {
    demoToggle.classList.toggle("is-paused", demo.paused);
    demoToggle.setAttribute("aria-label", demo.paused ? "Play demo" : "Pause demo");
  };

  demoToggle.addEventListener("click", () => {
    pausedByUser = !demo.paused;

    if (demo.paused) {
      demo.play().catch(() => {});
    } else {
      demo.pause();
    }
  });

  demo.addEventListener("play", sync);
  demo.addEventListener("pause", sync);

  if (pausedByUser) {
    demo.pause();
  }

  new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) {
          demo.pause();
        } else if (!pausedByUser) {
          demo.play().catch(() => {});
        }
      }
    },
    { threshold: 0.2 }
  ).observe(demo);

  sync();
}
