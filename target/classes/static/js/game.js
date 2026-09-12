/**
 * game.js - Main Application State and Game Logic Controller
 * Orchestrates API communication, UI rendering, board 3D card flips, and timer management.
 */

const GameApp = {
  user: null,
  session: null,
  timerInterval: null,
  previewInterval: null,
  isProcessing: false,

  selectedDifficulty: "EASY",
  selectedTheme: "animals",
  selectedMode: "CLASSIC",

  init() {
    AudioManager.init();
    UI.init();
    this.bindEvents();
    this.restoreUserSession();
  },

  bindEvents() {
    // Auth Tab Switching
    document
      .getElementById("tab-login")
      ?.addEventListener("click", () => this.switchAuthTab("login"));
    document
      .getElementById("tab-register")
      ?.addEventListener("click", () => this.switchAuthTab("register"));

    // Auth Form Submissions
    document
      .getElementById("form-login")
      ?.addEventListener("submit", (e) => this.handleLogin(e));
    document
      .getElementById("form-register")
      ?.addEventListener("submit", (e) => this.handleRegister(e));

    // Navigation
    document
      .getElementById("user-badge-container")
      ?.addEventListener("click", () => this.openProfile());
    document
      .getElementById("btn-nav-menu")
      ?.addEventListener("click", () => UI.showScreen("menu"));
    document
      .getElementById("btn-back-to-menu")
      ?.addEventListener("click", () => UI.showScreen("menu"));
    document
      .getElementById("btn-profile-back")
      ?.addEventListener("click", () => UI.showScreen("menu"));
    document
      .getElementById("btn-logout")
      ?.addEventListener("click", () => this.logout());

    // Audio controls
    document
      .getElementById("btn-toggle-sound")
      ?.addEventListener("click", () => {
        AudioManager.toggleSound();
        UI.updateAudioControls();
      });
    document
      .getElementById("btn-toggle-music")
      ?.addEventListener("click", () => {
        AudioManager.toggleMusic();
        UI.updateAudioControls();
      });

    // Difficulty & Theme Selectors
    document.querySelectorAll(".difficulty-option").forEach((el) => {
      el.addEventListener("click", () => {
        document
          .querySelectorAll(".difficulty-option")
          .forEach((opt) => opt.classList.remove("selected"));
        el.classList.add("selected");
        this.selectedDifficulty = el.dataset.diff;
      });
    });

    document.querySelectorAll(".theme-option").forEach((el) => {
      el.addEventListener("click", () => {
        document
          .querySelectorAll(".theme-option")
          .forEach((opt) => opt.classList.remove("selected"));
        el.classList.add("selected");
        this.selectedTheme = el.dataset.theme;
      });
    });

    document.querySelectorAll(".mode-option").forEach((el) => {
      el.addEventListener("click", () => {
        document
          .querySelectorAll(".mode-option")
          .forEach((opt) => opt.classList.remove("selected"));
        el.classList.add("selected");
        this.selectedMode = el.dataset.mode;
      });
    });

    // Game Launchers
    document
      .getElementById("btn-start-game")
      ?.addEventListener("click", () => this.startNewGame());
    document
      .getElementById("btn-daily-challenge")
      ?.addEventListener("click", () => this.startDailyChallenge());
    document
      .getElementById("btn-resume-game")
      ?.addEventListener("click", () => this.loadActiveGame());

    // In-Game Controls
    document
      .getElementById("btn-pause-game")
      ?.addEventListener("click", () => this.togglePause());
    document
      .getElementById("btn-restart-game")
      ?.addEventListener("click", () => this.restartGame());
    document
      .getElementById("btn-hint")
      ?.addEventListener("click", () => this.getHint());
    document
      .getElementById("btn-save-game")
      ?.addEventListener("click", () => this.saveProgress());

    // Win Modal Actions
    document.getElementById("btn-play-again")?.addEventListener("click", () => {
      UI.hideWinModal();
      this.restartGame();
    });
    document.getElementById("btn-win-menu")?.addEventListener("click", () => {
      UI.hideWinModal();
      UI.showScreen("menu");
    });

    // Profile Update Form
    document
      .getElementById("form-profile")
      ?.addEventListener("submit", (e) => this.handleProfileUpdate(e));
  },

  // ── Authentication & Session Persistence ──────────────────────────────────

  restoreUserSession() {
    const stored = sessionStorage.getItem("memoryGame_user");
    if (stored) {
      try {
        this.user = JSON.parse(stored);
        UI.updateUserBadge(this.user);
        UI.showScreen("menu");
        this.checkActiveSavedGame();
        return;
      } catch (e) {
        sessionStorage.removeItem("memoryGame_user");
      }
    }
    UI.showScreen("auth");
  },

  switchAuthTab(tab) {
    document
      .getElementById("tab-login")
      ?.classList.toggle("active", tab === "login");
    document
      .getElementById("tab-register")
      ?.classList.toggle("active", tab === "register");
    document
      .getElementById("container-login")
      ?.classList.toggle("active", tab === "login");
    document
      .getElementById("container-register")
      ?.classList.toggle("active", tab === "register");
  },

  async handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;

    if (!username || !password) {
      UI.showToast("Please enter both username and password.", "error");
      return;
    }

    try {
      const resp = await API.login(username, password);
      this.setUser(resp);
      UI.showToast(
        `Welcome back, ${resp.displayName || resp.username}!`,
        "success",
      );
      UI.showScreen("menu");
      this.checkActiveSavedGame();
    } catch (err) {
      UI.showToast(err.message, "error");
    }
  },

  async handleRegister(e) {
    e.preventDefault();
    const username = document.getElementById("reg-username").value.trim();
    const password = document.getElementById("reg-password").value;
    const displayName = document
      .getElementById("reg-display-name")
      .value.trim();

    if (!username || !password) {
      UI.showToast("Username and password are required.", "error");
      return;
    }

    try {
      const resp = await API.register(username, password, displayName);
      this.setUser(resp);
      UI.showToast(
        "Account registered successfully! Welcome aboard.",
        "success",
      );
      UI.showScreen("menu");
    } catch (err) {
      UI.showToast(err.message, "error");
    }
  },

  setUser(user) {
    this.user = user;
    sessionStorage.setItem("memoryGame_user", JSON.stringify(user));
    UI.updateUserBadge(user);
  },

  logout() {
    this.stopTimer();
    this.stopPreviewCountdown();
    this.user = null;
    this.session = null;
    sessionStorage.removeItem("memoryGame_user");
    UI.updateUserBadge(null);
    UI.showScreen("auth");
    UI.showToast("Logged out.", "info");
  },

  // ── Profile View ──────────────────────────────────────────────────────────

  async openProfile() {
    if (!this.user) return;
    try {
      const profile = await API.getProfile(this.user.userId);
      document.getElementById("prof-username").textContent = profile.username;
      document.getElementById("prof-avatar-letter").textContent = (
        profile.displayName || profile.username
      )
        .charAt(0)
        .toUpperCase();
      document.getElementById("prof-display-name-input").value =
        profile.displayName || "";
      document.getElementById("prof-theme-select").value =
        profile.preferredTheme || "animals";
      document.getElementById("prof-games-played").textContent =
        profile.totalGamesPlayed || 0;
      document.getElementById("prof-member-since").textContent =
        profile.memberSince || "Today";

      // Load high scores
      await this.loadScoreHistory();
      await this.loadPerformanceHistory();
      await this.loadEngagementDashboard();
      UI.showScreen("profile");
    } catch (err) {
      UI.showToast("Failed to load profile: " + err.message, "error");
    }
  },

  async handleProfileUpdate(e) {
    e.preventDefault();
    const displayName = document
      .getElementById("prof-display-name-input")
      .value.trim();
    const preferredTheme = document.getElementById("prof-theme-select").value;

    try {
      const updated = await API.updateProfile(this.user.userId, {
        displayName,
        preferredTheme,
      });
      this.user.displayName = updated.displayName;
      this.setUser(this.user);
      UI.showToast("Profile updated successfully!", "success");
    } catch (err) {
      UI.showToast("Update failed: " + err.message, "error");
    }
  },

  async loadScoreHistory() {
    try {
      const scores = await API.getPerformanceHistory(this.user.userId);
      const tbody = document.getElementById("scores-table-body");
      if (!tbody) return;

      if (!scores || scores.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 24px;">No completed games yet. Go play a match!</td></tr>`;
        return;
      }

      tbody.innerHTML = scores
        .map(
          (s, idx) => `
                <tr>
                    <td style="font-weight: 700; color: var(--primary);">#${idx + 1}</td>
                    <td style="font-weight: 700; color: var(--accent-cyan); font-family: var(--font-display);">${s.finalScore}</td>
                    <td>${Number(s.accuracyPercent).toFixed(0)}%</td>
                    <td>${s.mistakeCount}</td>
                    <td>${s.maxStreak}</td>
                    <td>${Number(s.concentrationScore).toFixed(0)}</td>
                    <td style="color: var(--text-muted); font-size: 0.8rem;">${new Date(s.sessionDate).toLocaleDateString()}</td>
                </tr>
            `,
        )
        .join("");
    } catch (err) {
      console.error("Failed to load score history:", err);
    }
  },

  async loadPerformanceHistory() {
    try {
      const history = await API.getPerformanceHistory(this.user.userId);
      this.drawProgressChart(history);
    } catch (err) {
      console.error("Failed to load performance history:", err);
    }
  },

  drawProgressChart(history) {
    const canvas = document.getElementById("progress-chart");
    if (!canvas || !history?.length) return;
    const context = canvas.getContext("2d");
    const width = canvas.clientWidth || 720;
    const height = 260;
    const ratio = window.devicePixelRatio || 1;
    canvas.width = width * ratio;
    canvas.height = height * ratio;
    context.setTransform(ratio, 0, 0, ratio, 0, 0);
    context.clearRect(0, 0, width, height);
    const points = [...history].reverse();
    const x = (index) =>
      36 + index * ((width - 60) / Math.max(1, points.length - 1));
    const y = (value) =>
      height - 28 - (Math.max(0, Math.min(100, value)) / 100) * (height - 52);
    context.strokeStyle = "rgba(255,255,255,.12)";
    [0, 25, 50, 75, 100].forEach((value) => {
      context.beginPath();
      context.moveTo(32, y(value));
      context.lineTo(width - 18, y(value));
      context.stroke();
    });
    const drawLine = (key, color) => {
      context.beginPath();
      points.forEach((point, index) =>
        index
          ? context.lineTo(x(index), y(point[key]))
          : context.moveTo(x(index), y(point[key])),
      );
      context.strokeStyle = color;
      context.lineWidth = 3;
      context.stroke();
      points.forEach((point, index) => {
        context.fillStyle = color;
        context.beginPath();
        context.arc(x(index), y(point[key]), 4, 0, Math.PI * 2);
        context.fill();
      });
    };
    drawLine("accuracyPercent", "#22d3ee");
    drawLine("concentrationScore", "#f59e0b");
  },

  async loadEngagementDashboard() {
    try {
      const [daily, badges, recommendations] = await Promise.all([
        API.getDailyChallenge(this.user.userId),
        API.getBadges(this.user.userId),
        API.getRecommendations(this.user.userId),
      ]);
      const dailyEl = document.getElementById("daily-challenge-status");
      if (dailyEl)
        dailyEl.textContent = daily.completed
          ? `Completed · ${daily.completionScore} pts`
          : "Ready to play";
      const badgesEl = document.getElementById("badge-list");
      if (badgesEl)
        badgesEl.innerHTML = badges.length
          ? badges
              .map(
                (badge) =>
                  `<span class="badge-pill" title="${badge.description}">🏅 ${badge.name}</span>`,
              )
              .join("")
          : '<span class="muted">No badges yet</span>';
      const recommendationsEl = document.getElementById("recommendation-list");
      if (recommendationsEl)
        recommendationsEl.innerHTML = recommendations
          .map(
            (item) =>
              `<li><strong>${item.title}</strong><span>${item.message}</span></li>`,
          )
          .join("");
    } catch (err) {
      console.error("Failed to load engagement dashboard:", err);
    }
  },

  // ── Game Management ───────────────────────────────────────────────────────

  async checkActiveSavedGame() {
    if (!this.user) return;
    try {
      const activeSession = await API.loadGame(this.user.userId);
      const resumeBtn = document.getElementById("btn-resume-game");
      if (resumeBtn) {
        resumeBtn.style.display = "inline-flex";
        resumeBtn.textContent = `▶ Resume ${activeSession.difficulty} Game`;
      }
    } catch (e) {
      const resumeBtn = document.getElementById("btn-resume-game");
      if (resumeBtn) resumeBtn.style.display = "none";
    }
  },

  async startNewGame() {
    if (!this.user) {
      UI.showScreen("auth");
      return;
    }

    try {
      const session = await API.startGame(
        this.user.userId,
        this.selectedDifficulty,
        this.selectedTheme,
        document.getElementById("focus-mode-toggle")?.checked || false,
        this.selectedMode,
      );
      this.setGameSession(session);
      UI.showScreen("game");
      UI.showToast(session.message || "Game started! Good luck.", "info");
    } catch (err) {
      UI.showToast("Could not start game: " + err.message, "error");
    }
  },

  async startDailyChallenge() {
    if (!this.user) return;
    try {
      const date = new Date().toISOString().slice(0, 10);
      const session = await API.startGame(this.user.userId, "EASY", "animals", false, "CLASSIC", true, date);
      this.setGameSession(session);
      UI.showScreen("game");
      UI.showToast("Today's fixed challenge is ready.", "info");
    } catch (err) {
      UI.showToast("Could not start daily challenge: " + err.message, "error");
    }
  },

  async loadActiveGame() {
    if (!this.user) return;
    try {
      const session = await API.loadGame(this.user.userId);
      this.setGameSession(session);
      UI.showScreen("game");
      UI.showToast(session.message || "Resumed saved game.", "info");
    } catch (err) {
      UI.showToast("No active game found.", "error");
      document.getElementById("btn-resume-game").style.display = "none";
    }
  },

  setGameSession(session) {
    this.session = session;
    document.body.classList.toggle("focus-mode", Boolean(session.focusMode));
    this.renderBoard(session);
    this.updateHUD(session);

    if (session.previewing) {
      this.startPreviewCountdown();
    } else if (session.sequencePlaybackActive) {
      this.startSequencePlayback(session);
    } else {
      this.stopPreviewCountdown();
    }

    if (session.status === "ACTIVE") {
      this.startTimer();
    } else {
      this.stopTimer();
    }
  },

  renderBoard(session) {
    const boardEl = document.getElementById("board-grid");
    if (!boardEl) return;

    boardEl.className = `board-grid ${session.difficulty.toLowerCase()} ${
      session.mode === "SEQUENCE_MEMORY" ? "sequence-mode" : ""
    }`;
    boardEl.innerHTML = "";

    session.board.forEach((card) => {
      const tile = document.createElement("div");
      tile.className = "card-tile";
      tile.dataset.cardId = card.cardId;

      if (
        card.flipped ||
        ((session.previewing || session.sequencePlaybackActive) &&
          card.symbolKey)
      )
        tile.classList.add("flipped");
      if (session.sequencePlaybackActive && session.sequencePlaybackCardIds) {
        const sequenceIndex = session.sequencePlaybackCardIds.indexOf(
          card.cardId,
        );
        if (sequenceIndex >= 0) {
          tile.classList.add("sequence-playback-step");
          tile.style.animationDelay = `${sequenceIndex * 120}ms`;
        }
      }
      if (card.matched) tile.classList.add("matched");

      tile.innerHTML = `
                <div class="card-inner">
                    <div class="card-back"></div>
                    <div class="card-face">${card.symbolKey || ""}</div>
                </div>
            `;

      tile.addEventListener("click", () => this.handleCardClick(card.cardId));
      boardEl.appendChild(tile);
    });
  },

  updateHUD(session) {
    document.getElementById("hud-moves").textContent = session.moves;
    document.getElementById("hud-hints").textContent = session.hintsUsed;
    document.getElementById("hud-score").textContent = session.score || 0;
    document.getElementById("hud-timer").textContent = UI.formatTime(
      session.elapsedSeconds,
    );
    document.getElementById("hud-accuracy").textContent =
      `${Number(session.accuracyPercent || 0).toFixed(0)}%`;
    document.getElementById("hud-streak").textContent =
      session.currentStreak || 0;
    document.getElementById("hud-reaction").textContent =
      `${((session.averageReactionTimeMillis || 0) / 1000).toFixed(1)}s`;
    document.getElementById("hud-move-limit").textContent =
      session.moveTimeLimitSeconds ? `${session.moveTimeLimitSeconds}s` : "--";
    const level = document.getElementById("hud-level");
    if (level) level.textContent = session.level || 1;
    document.getElementById("hud-timer").textContent =
      session.mode === "TIMED_CHALLENGE"
        ? UI.formatTime(session.timeRemainingSeconds || 0)
        : UI.formatTime(session.elapsedSeconds);

    const statusBanner = document.getElementById("status-banner");
    if (statusBanner && session.message) {
      statusBanner.textContent = session.message;
    }

    const pauseBtn = document.getElementById("btn-pause-game");
    if (pauseBtn) {
      if (session.status === "PAUSED") {
        pauseBtn.textContent = "▶ Resume";
        pauseBtn.className = "btn btn-accent";
      } else {
        pauseBtn.textContent = "⏸ Pause";
        pauseBtn.className = "btn btn-secondary";
      }
    }
  },

  // ── Card Flip Interaction ─────────────────────────────────────────────────

  async handleCardClick(cardId) {
    if (this.isProcessing) return;
    if (!this.session || this.session.status !== "ACTIVE") return;
    if (this.session.previewing) return;
    if (this.session.sequencePlaybackActive) return;

    const tileEl = document.querySelector(
      `.card-tile[data-card-id="${cardId}"]`,
    );
    if (!tileEl) return;

    if (
      tileEl.classList.contains("flipped") ||
      tileEl.classList.contains("matched")
    ) {
      return;
    }

    try {
      this.isProcessing = true;
      AudioManager.playFlip();

      // Optimistic flip animation
      tileEl.classList.add("flipped");

      const updatedSession = await API.flipCard(this.session.sessionId, cardId);
      this.session = updatedSession;
      this.updateHUD(updatedSession);

      if (updatedSession.status === "LOST") {
        this.stopTimer();
        UI.showResultsModal(updatedSession, false);
        this.isProcessing = false;
        return;
      }

      if (updatedSession.mode === "SEQUENCE_MEMORY") {
        this.renderBoard(updatedSession);
        this.isProcessing = false;
        if (updatedSession.wonGame) {
          AudioManager.playWin();
          setTimeout(() => UI.showWinModal(updatedSession), 600);
        } else if (updatedSession.flipResult === "SEQUENCE_MISMATCH") {
          AudioManager.playNoMatch();
        } else {
          AudioManager.playMatch();
        }
        return;
      }

      if (updatedSession.levelCompleted) {
        this.renderBoard(updatedSession);
        this.isProcessing = false;
        UI.showToast(
          `Level ${updatedSession.level - 1} complete. Level ${updatedSession.level} begins!`,
          "success",
        );
        return;
      }

      // Update card face(s) with the returned symbol. On a NO_MATCH the
      // server already flips both cards back down (and re-masks them)
      // before responding, so `updatedSession.board` alone would show
      // this card's symbol as null — it would rotate but stay blank.
      // `revealedThisMove` carries the true symbol(s) for the card(s)
      // involved in this move regardless of their post-move hidden
      // state, so both the first and second card render correctly.
      const revealIds =
        updatedSession.revealedThisMove &&
        updatedSession.revealedThisMove.length
          ? updatedSession.revealedThisMove
          : [cardId];

      revealIds.forEach((id) => {
        const el = document.querySelector(`.card-tile[data-card-id="${id}"]`);
        const face = el?.querySelector(".card-face");
        const data = updatedSession.board.find((c) => c.cardId === id);
        if (el && !el.classList.contains("flipped")) {
          el.classList.add("flipped");
        }
        if (face && data && data.symbolKey) {
          face.textContent = data.symbolKey;
        }
      });

      if (updatedSession.flipResult === "MATCH") {
        AudioManager.playMatch();
        // Mark both matched
        updatedSession.board.forEach((c) => {
          if (c.matched) {
            const el = document.querySelector(
              `.card-tile[data-card-id="${c.cardId}"]`,
            );
            if (el) {
              el.classList.add("flipped", "matched");
              const f = el.querySelector(".card-face");
              if (f && c.symbolKey) f.textContent = c.symbolKey;
            }
          }
        });

        if (updatedSession.wonGame) {
          this.stopTimer();
          AudioManager.playWin();
          setTimeout(() => {
            UI.showWinModal(updatedSession);
          }, 600);
        }

        this.isProcessing = false;
      } else if (updatedSession.flipResult === "NO_MATCH") {
        AudioManager.playNoMatch();
        // Shake and flip back after brief visual pause
        document
          .querySelectorAll(".card-tile.flipped:not(.matched)")
          .forEach((el) => {
            el.classList.add("mismatch-shake");
          });

        setTimeout(() => {
          this.renderBoard(updatedSession);
          this.isProcessing = false;
        }, 850);
      } else {
        // FIRST_FLIP
        this.isProcessing = false;
      }
    } catch (err) {
      UI.showToast(err.message, "error");
      tileEl.classList.remove("flipped");
      try {
        const refreshed = await API.getSession(this.session.sessionId);
        this.setGameSession(refreshed);
      } catch (refreshError) {
        console.error(
          "Failed to refresh session after rejected move:",
          refreshError,
        );
      }
      this.isProcessing = false;
    }
  },

  // ── In-Game Actions ───────────────────────────────────────────────────────

  async togglePause() {
    if (!this.session) return;
    try {
      if (this.session.status === "ACTIVE") {
        const updated = await API.pauseGame(this.session.sessionId);
        this.session = updated;
        this.stopTimer();
        this.updateHUD(updated);
        UI.showToast("Game paused.", "info");
      } else if (this.session.status === "PAUSED") {
        const updated = await API.resumeGame(this.session.sessionId);
        this.session = updated;
        this.startTimer();
        this.updateHUD(updated);
        UI.showToast("Game resumed.", "info");
      }
    } catch (err) {
      UI.showToast(err.message, "error");
    }
  },

  async restartGame() {
    if (!this.session) return;
    try {
      const updated = await API.restartGame(this.session.sessionId);
      this.setGameSession(updated);
      UI.showToast("Board reshuffled! New game started.", "info");
    } catch (err) {
      UI.showToast("Could not restart: " + err.message, "error");
    }
  },

  async getHint() {
    if (!this.session || this.session.status !== "ACTIVE") return;
    try {
      const hint = await API.getHint(this.session.sessionId);
      UI.showToast(
        `Hint used! Penalty: -${hint.scorePenaltyApplied} pts.`,
        "info",
      );

      // Highlight the matching pair
      hint.hintCardIds.forEach((id) => {
        const tile = document.querySelector(`.card-tile[data-card-id="${id}"]`);
        if (tile) tile.classList.add("hint-highlight");
      });

      // Update hints counter
      this.session.hintsUsed = hint.hintsUsed;
      document.getElementById("hud-hints").textContent = hint.hintsUsed;

      // Remove highlight after 1.8 seconds
      setTimeout(() => {
        hint.hintCardIds.forEach((id) => {
          const tile = document.querySelector(
            `.card-tile[data-card-id="${id}"]`,
          );
          if (tile) tile.classList.remove("hint-highlight");
        });
      }, 1800);
    } catch (err) {
      UI.showToast(err.message, "error");
    }
  },

  async saveProgress() {
    if (!this.session) return;
    try {
      const saved = await API.saveGame(this.session.sessionId);
      this.session = saved;
      UI.showToast("Progress saved successfully!", "success");
    } catch (err) {
      UI.showToast("Failed to save progress: " + err.message, "error");
    }
  },

  // ── Local Live Timer ──────────────────────────────────────────────────────

  startTimer() {
    this.stopTimer();
    this.timerInterval = setInterval(() => {
      if (this.session && this.session.status === "ACTIVE") {
        this.session.elapsedSeconds++;
        if (this.session.mode === "TIMED_CHALLENGE") {
          this.pollTimedSession();
        } else {
          document.getElementById("hud-timer").textContent = UI.formatTime(
            this.session.elapsedSeconds,
          );
        }
        this.updateMoveCountdown();
      }
    }, 1000);
  },

  stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  },

  async pollTimedSession() {
    if (!this.session || this.session.mode !== "TIMED_CHALLENGE") return;
    try {
      const refreshed = await API.getSession(this.session.sessionId);
      this.setGameSession(refreshed);
      if (refreshed.status === "LOST") {
        this.stopTimer();
        setTimeout(() => UI.showResultsModal(refreshed, false), 250);
        UI.showToast("Time expired. Challenge over.", "error");
      }
    } catch (err) {
      console.error("Timed session refresh failed:", err);
    }
  },

  startPreviewCountdown() {
    this.stopPreviewCountdown();
    const overlay = document.getElementById("preview-overlay");
    const countdown = document.getElementById("preview-countdown");
    if (!overlay || !countdown || !this.session?.previewEndsAt) return;

    overlay.hidden = false;
    const update = async () => {
      const remaining = Math.max(
        0,
        Math.ceil((Date.parse(this.session.previewEndsAt) - Date.now()) / 1000),
      );
      countdown.textContent = remaining;
      if (remaining === 0) {
        this.stopPreviewCountdown();
        try {
          const refreshed = await API.getSession(this.session.sessionId);
          this.setGameSession(refreshed);
        } catch (err) {
          UI.showToast(
            "Preview ended, but the board could not refresh.",
            "error",
          );
        }
      }
    };
    update();
    this.previewInterval = setInterval(update, 250);
  },

  startSequencePlayback(session) {
    this.stopPreviewCountdown();
    const overlay = document.getElementById("preview-overlay");
    const countdown = document.getElementById("preview-countdown");
    const kicker = document.getElementById("preview-kicker");
    if (!overlay || !countdown || !session.sequencePlaybackEndsAt) return;
    kicker.textContent = "Sequence playback";
    overlay.hidden = false;
    const update = async () => {
      const remaining = Math.max(
        0,
        Math.ceil(
          (Date.parse(session.sequencePlaybackEndsAt) - Date.now()) / 1000,
        ),
      );
      countdown.textContent = remaining;
      if (remaining === 0) {
        this.stopPreviewCountdown();
        try {
          const refreshed = await API.getSession(session.sessionId);
          this.setGameSession(refreshed);
        } catch (err) {
          UI.showToast("Sequence playback could not refresh.", "error");
        }
      }
    };
    update();
    this.previewInterval = setInterval(update, 250);
  },

  stopPreviewCountdown() {
    if (this.previewInterval) {
      clearInterval(this.previewInterval);
      this.previewInterval = null;
    }
    const overlay = document.getElementById("preview-overlay");
    if (overlay) {
      overlay.hidden = true;
      const kicker = document.getElementById("preview-kicker");
      if (kicker) kicker.textContent = "Memory preview";
    }
  },

  updateMoveCountdown() {
    const timer = document.getElementById("hud-move-limit");
    if (!timer || !this.session?.moveDeadlineAt) return;
    const remaining = Math.max(
      0,
      Math.ceil((Date.parse(this.session.moveDeadlineAt) - Date.now()) / 1000),
    );
    timer.textContent = `${remaining}s`;
    timer.classList.toggle("timer-warning", remaining <= 2);
  },
};

// Initialize Application once DOM is ready
document.addEventListener("DOMContentLoaded", () => {
  GameApp.init();
});
