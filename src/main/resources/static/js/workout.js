/**
 * workout.js - Daily Brain Workout feature.
 *
 * A self-contained module layered on top of the existing memory-card game.
 * It talks to the backend purely through API.getDailyWorkout / submitWorkoutAttempt /
 * getWorkoutProgress, and reuses the existing GameApp.user for identity and
 * UI.showScreen / UI.showToast for navigation — but does not touch game.js's
 * own match-3 game state at all, so it can't break the existing game screen.
 */

const DailyWorkout = {
  today: null, // last response from API.getDailyWorkout
  timers: [], // active interval/timeout ids to clear on exit
  runState: null, // per-mini-game working state

  init() {
    document
      .getElementById("btn-start-workout")
      ?.addEventListener("click", () => this.startWorkout());
    document
      .getElementById("btn-view-progress")
      ?.addEventListener("click", () => this.toggleProgress());
    document
      .getElementById("btn-workout-retry")
      ?.addEventListener("click", () => this.open());
    document
      .getElementById("btn-workout-done")
      ?.addEventListener("click", () => {
        this.clearTimers();
        UI.showScreen("menu");
      });
  },

  clearTimers() {
    this.timers.forEach((id) => {
      clearInterval(id);
      clearTimeout(id);
    });
    this.timers = [];
  },

  async open() {
    const user = GameApp?.user;
    if (!user) {
      UI.showToast("Please log in first.", "error");
      return;
    }
    this.clearTimers();
    document.getElementById("workout-results").hidden = true;
    document.getElementById("workout-progress").hidden = true;
    document.getElementById("workout-play-area").hidden = true;
    document.getElementById("workout-play-area").innerHTML = "";
    document.getElementById("workout-intro").hidden = false;

    try {
      this.today = await API.getDailyWorkout(user.userId);
      this.renderIntro(this.today);
      UI.showScreen("workout");
    } catch (err) {
      UI.showToast("Couldn't load today's workout: " + err.message, "error");
    }
  },

  renderIntro(data) {
    document.getElementById("workout-kicker").textContent = data.weekendBonus
      ? "Weekend Bonus — Mixed Challenge"
      : "Today's Focus";
    document.getElementById("workout-skill-name").textContent =
      data.skillLabel;
    document.getElementById("workout-description").textContent =
      data.description;
    document.getElementById("workout-stars").textContent = "⭐".repeat(
      data.levelStars || 1,
    );
    document.getElementById("workout-est-minutes").textContent =
      data.estimatedMinutes;
    document.getElementById("workout-streak-pill").textContent =
      `🔥 ${data.currentStreakDays}-day streak`;

    const status = document.getElementById("workout-status");
    if (data.completedToday) {
      status.innerHTML = `✅ Completed today — score <strong>${data.todayScore}</strong>${
        data.personalBest != null
          ? ` · Personal best: <strong>${data.personalBest}</strong>`
          : ""
      }`;
    } else if (data.personalBest != null) {
      status.innerHTML = `Not done yet today · Personal best: <strong>${data.personalBest}</strong>`;
    } else {
      status.textContent = "Not done yet today.";
    }

    document.getElementById("btn-start-workout").textContent =
      data.completedToday ? "🔄 Play Again" : "▶ Start Workout";
  },

  async toggleProgress() {
    const panel = document.getElementById("workout-progress");
    if (!panel.hidden) {
      panel.hidden = true;
      return;
    }
    const user = GameApp?.user;
    if (!user) return;
    try {
      const progress = await API.getWorkoutProgress(user.userId);
      this.renderProgress(progress);
      panel.hidden = false;
    } catch (err) {
      UI.showToast("Couldn't load progress: " + err.message, "error");
    }
  },

  renderProgress(progress) {
    const summary = document.getElementById("workout-progress-summary");
    const bestEntries = Object.entries(progress.personalBestBySkill || {});
    summary.innerHTML =
      `<span class="workout-meta-pill">Total workouts: <strong>${progress.totalWorkoutsCompleted}</strong></span>` +
      `<span class="workout-meta-pill">🔥 ${progress.currentStreakDays}-day streak</span>` +
      bestEntries
        .map(
          ([skill, best]) =>
            `<span class="workout-meta-pill">${skillLabel(skill)} best: <strong>${best}</strong></span>`,
        )
        .join("");

    const list = document.getElementById("workout-progress-list");
    if (!progress.recentHistory || progress.recentHistory.length === 0) {
      list.innerHTML = `<li class="workout-progress-empty">No workouts completed yet.</li>`;
      return;
    }
    list.innerHTML = progress.recentHistory
      .map(
        (h) => `
        <li class="workout-progress-item">
          <span>${h.date}</span>
          <span>${h.skillLabel}</span>
          <span>${h.level}</span>
          <span>${h.score} pts${h.personalBest ? " 🏆" : ""}</span>
        </li>`,
      )
      .join("");
  },

  // ── Dispatch ─────────────────────────────────────────────────────────────

  startWorkout() {
    if (!this.today) return;
    document.getElementById("workout-intro").hidden = true;
    document.getElementById("workout-results").hidden = true;
    const area = document.getElementById("workout-play-area");
    area.hidden = false;
    area.innerHTML = "";

    const challenge = this.today.challenge;
    switch (this.today.skill) {
      case "WORKING_MEMORY":
        this.runWorkingMemory(challenge);
        break;
      case "ATTENTION":
        this.runAttention(challenge);
        break;
      case "PROCESSING_SPEED":
        this.runProcessingSpeed(challenge);
        break;
      case "SEQUENCE_MEMORY":
        this.runSequenceMemory(challenge);
        break;
      case "DELAYED_RECALL":
        this.runDelayedRecall(challenge);
        break;
      default:
        UI.showToast("Unknown workout type.", "error");
    }
  },

  async finishWorkout(metrics) {
    this.clearTimers();
    document.getElementById("workout-play-area").hidden = true;
    const user = GameApp?.user;

    const payload = {
      skill: this.today.skill,
      level: this.today.level,
      correctCount: metrics.correctCount || 0,
      incorrectCount: metrics.incorrectCount || 0,
      totalItems: metrics.totalItems || 0,
      timeTakenSeconds: Math.max(0, Math.round(metrics.timeTakenSeconds || 0)),
      avgReactionTimeMillis: metrics.avgReactionTimeMillis ?? null,
      matchesFound: metrics.matchesFound ?? null,
      distractionsIgnored: metrics.distractionsIgnored ?? null,
      sequenceLength: metrics.sequenceLength ?? null,
      retries: metrics.retries ?? null,
      immediateAccuracyPercent: metrics.immediateAccuracyPercent ?? null,
      delayedAccuracyPercent: metrics.delayedAccuracyPercent ?? null,
    };

    try {
      const result = await API.submitWorkoutAttempt(user.userId, payload);
      document.getElementById("workout-result-score").textContent =
        result.score;
      document.getElementById("workout-result-accuracy").textContent =
        `${Math.round(result.accuracyPercent)}%`;
      document.getElementById("workout-result-time").textContent =
        UI.formatTime(payload.timeTakenSeconds);
      document.getElementById("workout-result-message").textContent =
        (result.personalBest ? "🔥 Personal Best! " : "") + result.message;
      document.getElementById("workout-results").hidden = false;
    } catch (err) {
      UI.showToast("Couldn't save your result: " + err.message, "error");
      document.getElementById("workout-intro").hidden = false;
    }
  },

  // ── DAY 1: Working Memory ───────────────────────────────────────────────

  runWorkingMemory(challenge) {
    const area = document.getElementById("workout-play-area");
    const cols = Math.ceil(Math.sqrt(challenge.gridCells));
    const itemsByPosition = new Map(
      challenge.items.map((it) => [it.position, it.symbol]),
    );

    area.innerHTML = `
      <div class="workout-instructions">Memorize which card sits in which square. Board hides in <span id="wm-countdown">${challenge.previewSeconds}</span>s.</div>
      <div class="workout-grid" style="--wg-cols:${cols}">
        ${Array.from({ length: challenge.gridCells }, (_, i) => `
          <div class="workout-cell" data-pos="${i}">
            <span class="workout-cell-number">${i + 1}</span>
            <span class="workout-cell-symbol">${itemsByPosition.get(i) || ""}</span>
          </div>`).join("")}
      </div>
      <div id="wm-recall-tray" class="workout-tray" hidden></div>
      <div class="workout-actions">
        <button id="wm-submit" class="btn btn-primary" hidden>Submit Answers</button>
      </div>`;

    let remaining = challenge.previewSeconds;
    const countdownEl = document.getElementById("wm-countdown");
    const timer = setInterval(() => {
      remaining--;
      if (countdownEl) countdownEl.textContent = String(Math.max(remaining, 0));
      if (remaining <= 0) {
        clearInterval(timer);
        this.beginWorkingMemoryRecall(challenge);
      }
    }, 1000);
    this.timers.push(timer);
  },

  beginWorkingMemoryRecall(challenge) {
    const area = document.getElementById("workout-play-area");
    area.querySelector(".workout-instructions").textContent =
      "Now place each card back where you remember it. Tap a symbol, then tap its square.";
    area.querySelectorAll(".workout-cell-symbol").forEach((el) => {
      el.textContent = "";
    });
    area.querySelectorAll(".workout-cell").forEach((cell) => {
      cell.classList.add("workout-cell-blank");
    });

    const shuffledSymbols = challenge.items
      .map((it) => it.symbol)
      .sort(() => Math.random() - 0.5);

    const tray = document.getElementById("wm-recall-tray");
    tray.hidden = false;
    tray.innerHTML = shuffledSymbols
      .map((s, i) => `<button class="workout-chip" data-symbol="${s}" data-idx="${i}">${s}</button>`)
      .join("");

    const placements = new Map(); // position -> symbol
    let selectedChip = null;
    const startTime = Date.now();

    tray.querySelectorAll(".workout-chip").forEach((chip) => {
      chip.addEventListener("click", () => {
        if (chip.disabled) return;
        tray.querySelectorAll(".workout-chip").forEach((c) => c.classList.remove("selected"));
        chip.classList.add("selected");
        selectedChip = chip;
      });
    });

    area.querySelectorAll(".workout-cell").forEach((cell) => {
      cell.addEventListener("click", () => {
        if (!selectedChip) return;
        const pos = Number(cell.dataset.pos);
        const symbol = selectedChip.dataset.symbol;
        placements.set(pos, symbol);
        cell.querySelector(".workout-cell-symbol").textContent = symbol;
        selectedChip.disabled = true;
        selectedChip.classList.add("placed");
        selectedChip = null;

        const submitBtn = document.getElementById("wm-submit");
        if (placements.size === challenge.items.length) {
          submitBtn.hidden = false;
        }
      });
    });

    document.getElementById("wm-submit").addEventListener("click", () => {
      let correct = 0;
      challenge.items.forEach((item) => {
        if (placements.get(item.position) === item.symbol) correct++;
      });
      const total = challenge.items.length;
      this.finishWorkout({
        correctCount: correct,
        incorrectCount: total - correct,
        totalItems: total,
        timeTakenSeconds: (Date.now() - startTime) / 1000,
      });
    });
  },

  // ── DAY 2: Attention ─────────────────────────────────────────────────────

  runAttention(challenge) {
    const area = document.getElementById("workout-play-area");
    const cols = Math.min(6, Math.ceil(Math.sqrt(challenge.cards.length)));
    area.innerHTML = `
      <div class="workout-instructions">Find every matching pair. Some cards are decoys with no partner — don't waste picks on them.</div>
      <div class="workout-grid workout-attention-grid" style="--wg-cols:${cols}">
        ${challenge.cards.map((c) => `
          <div class="workout-cell workout-flip-card" data-id="${c.cardId}" data-symbol="${c.symbol}" data-decoy="${c.decoy}">
            <span class="workout-cell-symbol"></span>
          </div>`).join("")}
      </div>`;

    const totalPairs = challenge.cards.filter((c) => !c.decoy).length / 2;
    let matched = 0;
    let mistakes = 0;
    const decoysClicked = new Set();
    let firstCard = null;
    let locked = false;
    const startTime = Date.now();

    area.querySelectorAll(".workout-flip-card").forEach((cell) => {
      cell.addEventListener("click", () => {
        if (locked || cell.classList.contains("flipped") || cell.classList.contains("matched")) return;
        cell.classList.add("flipped");
        cell.querySelector(".workout-cell-symbol").textContent = cell.dataset.symbol;

        if (cell.dataset.decoy === "true") decoysClicked.add(cell.dataset.id);

        if (!firstCard) {
          firstCard = cell;
          return;
        }
        locked = true;
        const isMatch =
          firstCard.dataset.symbol === cell.dataset.symbol &&
          firstCard.dataset.decoy === "false" &&
          firstCard !== cell;
        setTimeout(() => {
          if (isMatch) {
            firstCard.classList.add("matched");
            cell.classList.add("matched");
            matched++;
          } else {
            firstCard.classList.remove("flipped");
            cell.classList.remove("flipped");
            firstCard.querySelector(".workout-cell-symbol").textContent = "";
            cell.querySelector(".workout-cell-symbol").textContent = "";
            mistakes++;
          }
          firstCard = null;
          locked = false;
          if (matched === totalPairs) {
            const totalDecoys = challenge.cards.filter((c) => c.decoy).length;
            this.finishWorkout({
              correctCount: matched,
              incorrectCount: mistakes,
              totalItems: matched + mistakes,
              distractionsIgnored: totalDecoys - decoysClicked.size,
              timeTakenSeconds: (Date.now() - startTime) / 1000,
            });
          }
        }, 700);
      });
    });
  },

  // ── DAY 3: Processing Speed ──────────────────────────────────────────────

  runProcessingSpeed(challenge) {
    const area = document.getElementById("workout-play-area");
    const cols = Math.min(6, Math.ceil(Math.sqrt(challenge.cards.length)));
    area.innerHTML = `
      <div class="workout-instructions">Match as many pairs as you can before time runs out. Accuracy still counts!</div>
      <div class="workout-meta-row"><span class="workout-meta-pill">⏱ <span id="ps-timer">${challenge.timeLimitSeconds}</span>s left</span></div>
      <div class="workout-grid workout-attention-grid" style="--wg-cols:${cols}">
        ${challenge.cards.map((c) => `
          <div class="workout-cell workout-flip-card" data-id="${c.cardId}" data-symbol="${c.symbol}">
            <span class="workout-cell-symbol"></span>
          </div>`).join("")}
      </div>`;

    const totalPairs = challenge.cards.length / 2;
    let matched = 0;
    let mistakes = 0;
    let firstCard = null;
    let locked = false;
    let ended = false;
    const startTime = Date.now();
    let remaining = challenge.timeLimitSeconds;
    const timerEl = document.getElementById("ps-timer");

    const finish = () => {
      if (ended) return;
      ended = true;
      this.finishWorkout({
        correctCount: matched,
        incorrectCount: mistakes,
        totalItems: matched + mistakes,
        matchesFound: matched,
        timeTakenSeconds: (Date.now() - startTime) / 1000,
      });
    };

    const countdown = setInterval(() => {
      remaining--;
      if (timerEl) timerEl.textContent = String(Math.max(remaining, 0));
      if (remaining <= 0) {
        clearInterval(countdown);
        finish();
      }
    }, 1000);
    this.timers.push(countdown);

    area.querySelectorAll(".workout-flip-card").forEach((cell) => {
      cell.addEventListener("click", () => {
        if (ended || locked || cell.classList.contains("flipped") || cell.classList.contains("matched")) return;
        cell.classList.add("flipped");
        cell.querySelector(".workout-cell-symbol").textContent = cell.dataset.symbol;

        if (!firstCard) {
          firstCard = cell;
          return;
        }
        locked = true;
        const isMatch = firstCard.dataset.symbol === cell.dataset.symbol && firstCard !== cell;
        setTimeout(() => {
          if (ended) return;
          if (isMatch) {
            firstCard.classList.add("matched");
            cell.classList.add("matched");
            matched++;
          } else {
            firstCard.classList.remove("flipped");
            cell.classList.remove("flipped");
            firstCard.querySelector(".workout-cell-symbol").textContent = "";
            cell.querySelector(".workout-cell-symbol").textContent = "";
            mistakes++;
          }
          firstCard = null;
          locked = false;
          if (matched === totalPairs) {
            clearInterval(countdown);
            finish();
          }
        }, 500);
      });
    });
  },

  // ── DAY 4: Sequence Memory ───────────────────────────────────────────────

  runSequenceMemory(challenge) {
    const area = document.getElementById("workout-play-area");
    const tileById = new Map(challenge.sequenceTiles.map((t) => [t.cardId, t.symbol]));
    const cols = Math.ceil(Math.sqrt(challenge.sequenceTiles.length));

    area.innerHTML = `
      <div class="workout-instructions" id="seq-instructions">Watch the sequence...</div>
      <div class="workout-grid" style="--wg-cols:${cols}">
        ${challenge.sequenceTiles.map((t) => `
          <div class="workout-cell workout-seq-tile" data-id="${t.cardId}">
            <span class="workout-cell-symbol">${t.symbol}</span>
          </div>`).join("")}
      </div>`;

    let sequence = [...challenge.sequence];
    let round = 0;
    let maxLength = 0;
    let correctClicks = 0;
    let incorrectClicks = 0;
    const startTime = Date.now();
    const tileEls = new Map(
      Array.from(area.querySelectorAll(".workout-seq-tile")).map((el) => [Number(el.dataset.id), el]),
    );

    const flash = (id) =>
      new Promise((resolve) => {
        const el = tileEls.get(id);
        el.classList.add("active");
        const t1 = setTimeout(() => {
          el.classList.remove("active");
          const t2 = setTimeout(resolve, 250);
          this.timers.push(t2);
        }, 550);
        this.timers.push(t1);
      });

    const playback = async () => {
      document.getElementById("seq-instructions").textContent = "Watch the sequence...";
      area.querySelectorAll(".workout-seq-tile").forEach((el) => (el.style.pointerEvents = "none"));
      for (const id of sequence) {
        await flash(id);
      }
      document.getElementById("seq-instructions").textContent =
        `Now repeat it! (${sequence.length} card${sequence.length > 1 ? "s" : ""})`;
      area.querySelectorAll(".workout-seq-tile").forEach((el) => (el.style.pointerEvents = "auto"));
      round++;
    };

    let inputIndex = 0;
    const finish = () => {
      this.finishWorkout({
        correctCount: correctClicks,
        incorrectCount: incorrectClicks,
        totalItems: correctClicks + incorrectClicks,
        sequenceLength: maxLength,
        retries: round,
        timeTakenSeconds: (Date.now() - startTime) / 1000,
      });
    };

    area.querySelectorAll(".workout-seq-tile").forEach((el) => {
      el.addEventListener("click", () => {
        if (el.style.pointerEvents === "none") return;
        const id = Number(el.dataset.id);
        if (id === sequence[inputIndex]) {
          correctClicks++;
          el.classList.add("workout-seq-correct");
          setTimeout(() => el.classList.remove("workout-seq-correct"), 300);
          inputIndex++;
          if (inputIndex === sequence.length) {
            maxLength = Math.max(maxLength, sequence.length);
            inputIndex = 0;
            // extend the sequence by one more random tile and play again
            const nextId = challenge.sequenceTiles[
              Math.floor(Math.random() * challenge.sequenceTiles.length)
            ].cardId;
            sequence = [...sequence, nextId];
            const t = setTimeout(() => playback(), 600);
            this.timers.push(t);
          }
        } else {
          incorrectClicks++;
          el.classList.add("workout-seq-wrong");
          setTimeout(() => el.classList.remove("workout-seq-wrong"), 300);
          maxLength = Math.max(maxLength, sequence.length - 1);
          area.querySelectorAll(".workout-seq-tile").forEach((e) => (e.style.pointerEvents = "none"));
          document.getElementById("seq-instructions").textContent =
            "Not quite — that ends the round!";
          const t = setTimeout(() => finish(), 900);
          this.timers.push(t);
        }
      });
    });

    playback();
  },

  // ── DAY 5: Delayed Recall ────────────────────────────────────────────────

  runDelayedRecall(challenge) {
    const area = document.getElementById("workout-play-area");
    const cols = Math.ceil(Math.sqrt(challenge.gridCells));
    const itemsByPosition = new Map(challenge.items.map((it) => [it.position, it.symbol]));

    area.innerHTML = `
      <div class="workout-instructions">Study these cards closely. You'll be asked about them after a short break. Hiding in <span id="dr-countdown">${challenge.previewSeconds}</span>s.</div>
      <div class="workout-grid" style="--wg-cols:${cols}">
        ${Array.from({ length: challenge.gridCells }, (_, i) => `
          <div class="workout-cell" data-pos="${i}">
            <span class="workout-cell-number">${i + 1}</span>
            <span class="workout-cell-symbol">${itemsByPosition.get(i) || ""}</span>
          </div>`).join("")}
      </div>`;

    let remaining = challenge.previewSeconds;
    const countdownEl = document.getElementById("dr-countdown");
    const timer = setInterval(() => {
      remaining--;
      if (countdownEl) countdownEl.textContent = String(Math.max(remaining, 0));
      if (remaining <= 0) {
        clearInterval(timer);
        this.beginDelayedRecallFiller(challenge);
      }
    }, 1000);
    this.timers.push(timer);
  },

  beginDelayedRecallFiller(challenge) {
    const area = document.getElementById("workout-play-area");
    const fillerPrompts = [
      "7 + 5",
      "12 - 4",
      "3 × 6",
      "9 + 8",
      "15 - 7",
    ].sort(() => Math.random() - 0.5);

    area.innerHTML = `
      <div class="workout-instructions">Quick warm-up while your memory settles — solve a couple of these in your head.</div>
      <div class="workout-filler">
        ${fillerPrompts.slice(0, 2).map((p) => `<div class="workout-filler-prompt">${p} = ?</div>`).join("")}
      </div>
      <div class="workout-meta-row"><span class="workout-meta-pill">Recall unlocks in <span id="dr-delay-countdown">${challenge.delaySeconds}</span>s</span></div>
      <div class="workout-actions">
        <button id="dr-continue" class="btn btn-primary" disabled>Continue to Recall</button>
      </div>`;

    let remaining = challenge.delaySeconds;
    const el = document.getElementById("dr-delay-countdown");
    const btn = document.getElementById("dr-continue");
    const timer = setInterval(() => {
      remaining--;
      if (el) el.textContent = String(Math.max(remaining, 0));
      if (remaining <= 0) {
        clearInterval(timer);
        btn.disabled = false;
        btn.textContent = "Continue to Recall →";
      }
    }, 1000);
    this.timers.push(timer);

    btn.addEventListener("click", () => this.beginDelayedRecallQuestions(challenge));
  },

  beginDelayedRecallQuestions(challenge) {
    const area = document.getElementById("workout-play-area");
    const questions = challenge.questions;
    let index = 0;
    let correct = 0;
    let incorrect = 0;
    const startTime = Date.now();

    const renderQuestion = () => {
      const q = questions[index];
      area.innerHTML = `
        <div class="workout-instructions">Question ${index + 1} of ${questions.length}</div>
        <div class="workout-recall-prompt">${q.prompt}</div>
        <div class="workout-recall-options">
          ${q.options.map((opt) => `<button class="btn btn-secondary workout-recall-option" data-answer="${opt}">${opt}</button>`).join("")}
        </div>`;

      area.querySelectorAll(".workout-recall-option").forEach((btn) => {
        btn.addEventListener("click", () => {
          area.querySelectorAll(".workout-recall-option").forEach((b) => (b.disabled = true));
          const isCorrect = btn.dataset.answer === q.correctAnswer;
          btn.classList.add(isCorrect ? "workout-answer-correct" : "workout-answer-wrong");
          if (isCorrect) correct++;
          else incorrect++;

          setTimeout(() => {
            index++;
            if (index < questions.length) {
              renderQuestion();
            } else {
              const total = questions.length;
              this.finishWorkout({
                correctCount: correct,
                incorrectCount: incorrect,
                totalItems: total,
                delayedAccuracyPercent: (correct / total) * 100,
                timeTakenSeconds: (Date.now() - startTime) / 1000,
              });
            }
          }, 700);
        });
      });
    };

    renderQuestion();
  },
};

function skillLabel(skill) {
  const labels = {
    WORKING_MEMORY: "Working Memory",
    ATTENTION: "Attention",
    PROCESSING_SPEED: "Processing Speed",
    SEQUENCE_MEMORY: "Sequence Memory",
    DELAYED_RECALL: "Delayed Recall",
  };
  return labels[skill] || skill;
}

window.DailyWorkout = DailyWorkout;

document.addEventListener("DOMContentLoaded", () => {
  DailyWorkout.init();
});
