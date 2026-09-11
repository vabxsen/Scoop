const year = document.querySelector("#year");

if (year) {
  year.textContent = new Date().getFullYear().toString();
}

const configurator = document.querySelector("#configurator");

if (configurator) {
  const fileOut = configurator.querySelector("#cfg-file");
  const pathOut = configurator.querySelector("#cfg-path");
  const planOut = configurator.querySelector("#cfg-plan");

  const picked = (name) =>
    configurator.querySelector(`input[name="${name}"]:checked`).dataset.value;

  const flagsFor = (ids) =>
    ids
      .map((id) => configurator.querySelector(`#${id}`))
      .filter((box) => box.checked)
      .map((box) => box.dataset.flag);

  const videoSelector = {
    best: "-f bestvideo+bestaudio/best",
    1080: "-f bv*[height<=1080]+ba/b[height<=1080]",
    720: "-f bv*[height<=720]+ba/b[height<=720]",
    low: "-f worstvideo+worstaudio/worst",
  };

  const plans = {
    images: () => ({
      file: picked("cfg-scope") === "all" ? "sunset-01.png … sunset-24.png" : "sunset-07.png",
      path: "Pictures/Scoop",
      lines: [
        picked("cfg-scope") === "all"
          ? "queue all 24 results"
          : "queue only the images you ticked",
        "keep original bytes and extension",
        "no resize, no recompression",
      ],
    }),
    video: () => {
      const container = picked("cfg-container");

      return {
        file: `sunset.${container}`,
        path: "Movies/Scoop",
        lines: [
          videoSelector[picked("cfg-vq")],
          `--merge-output-format ${container}`,
          ...flagsFor(["cfg-subs", "cfg-vthumb"]),
        ],
      };
    },
    audio: () => {
      const format = picked("cfg-af");

      return {
        file: `sunset.${format}`,
        path: "Music/Scoop",
        lines: [
          `-x --audio-format ${format}`,
          `--audio-quality ${picked("cfg-aq")}`,
          ...flagsFor(["cfg-cover"]),
        ],
      };
    },
  };

  const render = () => {
    const mode = configurator.querySelector('input[name="cfg-mode"]:checked').dataset.mode;
    const plan = plans[mode]();

    fileOut.textContent = plan.file;
    pathOut.textContent = plan.path;
    planOut.textContent = plan.lines.join("\n");
  };

  configurator.addEventListener("change", render);
  render();
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
