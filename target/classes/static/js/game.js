/**
 * game.js - Main Application State and Game Logic Controller
 * Orchestrates API communication, UI rendering, board 3D card flips, and timer management.
 */

const GameApp = {
    user: null,
    session: null,
    timerInterval: null,
    isProcessing: false,

    selectedDifficulty: 'EASY',
    selectedTheme: 'animals',

    init() {
        AudioManager.init();
        UI.init();
        this.bindEvents();
        this.restoreUserSession();
    },

    bindEvents() {
        // Auth Tab Switching
        document.getElementById('tab-login')?.addEventListener('click', () => this.switchAuthTab('login'));
        document.getElementById('tab-register')?.addEventListener('click', () => this.switchAuthTab('register'));

        // Auth Form Submissions
        document.getElementById('form-login')?.addEventListener('submit', (e) => this.handleLogin(e));
        document.getElementById('form-register')?.addEventListener('submit', (e) => this.handleRegister(e));

        // Navigation
        document.getElementById('user-badge-container')?.addEventListener('click', () => this.openProfile());
        document.getElementById('btn-nav-menu')?.addEventListener('click', () => UI.showScreen('menu'));
        document.getElementById('btn-back-to-menu')?.addEventListener('click', () => UI.showScreen('menu'));
        document.getElementById('btn-profile-back')?.addEventListener('click', () => UI.showScreen('menu'));
        document.getElementById('btn-logout')?.addEventListener('click', () => this.logout());

        // Audio controls
        document.getElementById('btn-toggle-sound')?.addEventListener('click', () => {
            AudioManager.toggleSound();
            UI.updateAudioControls();
        });
        document.getElementById('btn-toggle-music')?.addEventListener('click', () => {
            AudioManager.toggleMusic();
            UI.updateAudioControls();
        });

        // Difficulty & Theme Selectors
        document.querySelectorAll('.difficulty-option').forEach(el => {
            el.addEventListener('click', () => {
                document.querySelectorAll('.difficulty-option').forEach(opt => opt.classList.remove('selected'));
                el.classList.add('selected');
                this.selectedDifficulty = el.dataset.diff;
            });
        });

        document.querySelectorAll('.theme-option').forEach(el => {
            el.addEventListener('click', () => {
                document.querySelectorAll('.theme-option').forEach(opt => opt.classList.remove('selected'));
                el.classList.add('selected');
                this.selectedTheme = el.dataset.theme;
            });
        });

        // Game Launchers
        document.getElementById('btn-start-game')?.addEventListener('click', () => this.startNewGame());
        document.getElementById('btn-resume-game')?.addEventListener('click', () => this.loadActiveGame());

        // In-Game Controls
        document.getElementById('btn-pause-game')?.addEventListener('click', () => this.togglePause());
        document.getElementById('btn-restart-game')?.addEventListener('click', () => this.restartGame());
        document.getElementById('btn-hint')?.addEventListener('click', () => this.getHint());
        document.getElementById('btn-save-game')?.addEventListener('click', () => this.saveProgress());

        // Win Modal Actions
        document.getElementById('btn-play-again')?.addEventListener('click', () => {
            UI.hideWinModal();
            this.restartGame();
        });
        document.getElementById('btn-win-menu')?.addEventListener('click', () => {
            UI.hideWinModal();
            UI.showScreen('menu');
        });

        // Profile Update Form
        document.getElementById('form-profile')?.addEventListener('submit', (e) => this.handleProfileUpdate(e));
    },

    // ── Authentication & Session Persistence ──────────────────────────────────

    restoreUserSession() {
        const stored = sessionStorage.getItem('memoryGame_user');
        if (stored) {
            try {
                this.user = JSON.parse(stored);
                UI.updateUserBadge(this.user);
                UI.showScreen('menu');
                this.checkActiveSavedGame();
                return;
            } catch (e) {
                sessionStorage.removeItem('memoryGame_user');
            }
        }
        UI.showScreen('auth');
    },

    switchAuthTab(tab) {
        document.getElementById('tab-login')?.classList.toggle('active', tab === 'login');
        document.getElementById('tab-register')?.classList.toggle('active', tab === 'register');
        document.getElementById('container-login')?.classList.toggle('active', tab === 'login');
        document.getElementById('container-register')?.classList.toggle('active', tab === 'register');
    },

    async handleLogin(e) {
        e.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;

        if (!username || !password) {
            UI.showToast('Please enter both username and password.', 'error');
            return;
        }

        try {
            const resp = await API.login(username, password);
            this.setUser(resp);
            UI.showToast(`Welcome back, ${resp.displayName || resp.username}!`, 'success');
            UI.showScreen('menu');
            this.checkActiveSavedGame();
        } catch (err) {
            UI.showToast(err.message, 'error');
        }
    },

    async handleRegister(e) {
        e.preventDefault();
        const username = document.getElementById('reg-username').value.trim();
        const password = document.getElementById('reg-password').value;
        const displayName = document.getElementById('reg-display-name').value.trim();

        if (!username || !password) {
            UI.showToast('Username and password are required.', 'error');
            return;
        }

        try {
            const resp = await API.register(username, password, displayName);
            this.setUser(resp);
            UI.showToast('Account registered successfully! Welcome aboard.', 'success');
            UI.showScreen('menu');
        } catch (err) {
            UI.showToast(err.message, 'error');
        }
    },

    setUser(user) {
        this.user = user;
        sessionStorage.setItem('memoryGame_user', JSON.stringify(user));
        UI.updateUserBadge(user);
    },

    logout() {
        this.stopTimer();
        this.user = null;
        this.session = null;
        sessionStorage.removeItem('memoryGame_user');
        UI.updateUserBadge(null);
        UI.showScreen('auth');
        UI.showToast('Logged out.', 'info');
    },

    // ── Profile View ──────────────────────────────────────────────────────────

    async openProfile() {
        if (!this.user) return;
        try {
            const profile = await API.getProfile(this.user.userId);
            document.getElementById('prof-username').textContent = profile.username;
            document.getElementById('prof-avatar-letter').textContent = (profile.displayName || profile.username).charAt(0).toUpperCase();
            document.getElementById('prof-display-name-input').value = profile.displayName || '';
            document.getElementById('prof-theme-select').value = profile.preferredTheme || 'animals';
            document.getElementById('prof-games-played').textContent = profile.totalGamesPlayed || 0;
            document.getElementById('prof-member-since').textContent = profile.memberSince || 'Today';

            // Load high scores
            await this.loadScoreHistory();
            UI.showScreen('profile');
        } catch (err) {
            UI.showToast('Failed to load profile: ' + err.message, 'error');
        }
    },

    async handleProfileUpdate(e) {
        e.preventDefault();
        const displayName = document.getElementById('prof-display-name-input').value.trim();
        const preferredTheme = document.getElementById('prof-theme-select').value;

        try {
            const updated = await API.updateProfile(this.user.userId, { displayName, preferredTheme });
            this.user.displayName = updated.displayName;
            this.setUser(this.user);
            UI.showToast('Profile updated successfully!', 'success');
        } catch (err) {
            UI.showToast('Update failed: ' + err.message, 'error');
        }
    },

    async loadScoreHistory() {
        try {
            const scores = await API.getScoreHistory(this.user.userId);
            const tbody = document.getElementById('scores-table-body');
            if (!tbody) return;

            if (!scores || scores.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--text-muted); padding: 24px;">No completed games yet. Go play a match!</td></tr>`;
                return;
            }

            tbody.innerHTML = scores.map((s, idx) => `
                <tr>
                    <td style="font-weight: 700; color: var(--primary);">#${idx + 1}</td>
                    <td><span class="user-badge" style="display:inline-block;">${s.difficulty}</span></td>
                    <td style="font-weight: 700; color: var(--accent-cyan); font-family: var(--font-display);">${s.score}</td>
                    <td>${s.moves}</td>
                    <td>${UI.formatTime(s.timeTakenSeconds)}</td>
                    <td style="color: var(--text-muted); font-size: 0.8rem;">${new Date(s.playedAt).toLocaleDateString()}</td>
                </tr>
            `).join('');
        } catch (err) {
            console.error('Failed to load score history:', err);
        }
    },

    // ── Game Management ───────────────────────────────────────────────────────

    async checkActiveSavedGame() {
        if (!this.user) return;
        try {
            const activeSession = await API.loadGame(this.user.userId);
            const resumeBtn = document.getElementById('btn-resume-game');
            if (resumeBtn) {
                resumeBtn.style.display = 'inline-flex';
                resumeBtn.textContent = `▶ Resume ${activeSession.difficulty} Game`;
            }
        } catch (e) {
            const resumeBtn = document.getElementById('btn-resume-game');
            if (resumeBtn) resumeBtn.style.display = 'none';
        }
    },

    async startNewGame() {
        if (!this.user) {
            UI.showScreen('auth');
            return;
        }

        try {
            const session = await API.startGame(
                this.user.userId,
                this.selectedDifficulty,
                this.selectedTheme
            );
            this.setGameSession(session);
            UI.showScreen('game');
            UI.showToast(session.message || 'Game started! Good luck.', 'info');
        } catch (err) {
            UI.showToast('Could not start game: ' + err.message, 'error');
        }
    },

    async loadActiveGame() {
        if (!this.user) return;
        try {
            const session = await API.loadGame(this.user.userId);
            this.setGameSession(session);
            UI.showScreen('game');
            UI.showToast(session.message || 'Resumed saved game.', 'info');
        } catch (err) {
            UI.showToast('No active game found.', 'error');
            document.getElementById('btn-resume-game').style.display = 'none';
        }
    },

    setGameSession(session) {
        this.session = session;
        this.renderBoard(session);
        this.updateHUD(session);

        if (session.status === 'ACTIVE') {
            this.startTimer();
        } else {
            this.stopTimer();
        }
    },

    renderBoard(session) {
        const boardEl = document.getElementById('board-grid');
        if (!boardEl) return;

        boardEl.className = `board-grid ${session.difficulty.toLowerCase()}`;
        boardEl.innerHTML = '';

        session.board.forEach(card => {
            const tile = document.createElement('div');
            tile.className = 'card-tile';
            tile.dataset.cardId = card.cardId;

            if (card.flipped) tile.classList.add('flipped');
            if (card.matched) tile.classList.add('matched');

            tile.innerHTML = `
                <div class="card-inner">
                    <div class="card-back"></div>
                    <div class="card-face">${card.symbolKey || ''}</div>
                </div>
            `;

            tile.addEventListener('click', () => this.handleCardClick(card.cardId));
            boardEl.appendChild(tile);
        });
    },

    updateHUD(session) {
        document.getElementById('hud-moves').textContent = session.moves;
        document.getElementById('hud-hints').textContent = session.hintsUsed;
        document.getElementById('hud-score').textContent = session.score || 0;
        document.getElementById('hud-timer').textContent = UI.formatTime(session.elapsedSeconds);

        const statusBanner = document.getElementById('status-banner');
        if (statusBanner && session.message) {
            statusBanner.textContent = session.message;
        }

        const pauseBtn = document.getElementById('btn-pause-game');
        if (pauseBtn) {
            if (session.status === 'PAUSED') {
                pauseBtn.textContent = '▶ Resume';
                pauseBtn.className = 'btn btn-accent';
            } else {
                pauseBtn.textContent = '⏸ Pause';
                pauseBtn.className = 'btn btn-secondary';
            }
        }
    },

    // ── Card Flip Interaction ─────────────────────────────────────────────────

    async handleCardClick(cardId) {
        if (this.isProcessing) return;
        if (!this.session || this.session.status !== 'ACTIVE') return;

        const tileEl = document.querySelector(`.card-tile[data-card-id="${cardId}"]`);
        if (!tileEl) return;

        if (tileEl.classList.contains('flipped') || tileEl.classList.contains('matched')) {
            return;
        }

        try {
            this.isProcessing = true;
            AudioManager.playFlip();

            // Optimistic flip animation
            tileEl.classList.add('flipped');

            const updatedSession = await API.flipCard(this.session.sessionId, cardId);
            this.session = updatedSession;
            this.updateHUD(updatedSession);

            // Update card face with returned symbol
            const faceEl = tileEl.querySelector('.card-face');
            const cardData = updatedSession.board.find(c => c.cardId === cardId);
            if (faceEl && cardData && cardData.symbolKey) {
                faceEl.textContent = cardData.symbolKey;
            }

            if (updatedSession.flipResult === 'MATCH') {
                AudioManager.playMatch();
                // Mark both matched
                updatedSession.board.forEach(c => {
                    if (c.matched) {
                        const el = document.querySelector(`.card-tile[data-card-id="${c.cardId}"]`);
                        if (el) {
                            el.classList.add('flipped', 'matched');
                            const f = el.querySelector('.card-face');
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

            } else if (updatedSession.flipResult === 'NO_MATCH') {
                AudioManager.playNoMatch();
                // Shake and flip back after brief visual pause
                document.querySelectorAll('.card-tile.flipped:not(.matched)').forEach(el => {
                    el.classList.add('mismatch-shake');
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
            UI.showToast(err.message, 'error');
            tileEl.classList.remove('flipped');
            this.isProcessing = false;
        }
    },

    // ── In-Game Actions ───────────────────────────────────────────────────────

    async togglePause() {
        if (!this.session) return;
        try {
            if (this.session.status === 'ACTIVE') {
                const updated = await API.pauseGame(this.session.sessionId);
                this.session = updated;
                this.stopTimer();
                this.updateHUD(updated);
                UI.showToast('Game paused.', 'info');
            } else if (this.session.status === 'PAUSED') {
                const updated = await API.resumeGame(this.session.sessionId);
                this.session = updated;
                this.startTimer();
                this.updateHUD(updated);
                UI.showToast('Game resumed.', 'info');
            }
        } catch (err) {
            UI.showToast(err.message, 'error');
        }
    },

    async restartGame() {
        if (!this.session) return;
        try {
            const updated = await API.restartGame(this.session.sessionId);
            this.setGameSession(updated);
            UI.showToast('Board reshuffled! New game started.', 'info');
        } catch (err) {
            UI.showToast('Could not restart: ' + err.message, 'error');
        }
    },

    async getHint() {
        if (!this.session || this.session.status !== 'ACTIVE') return;
        try {
            const hint = await API.getHint(this.session.sessionId);
            UI.showToast(`Hint used! Penalty: -${hint.scorePenaltyApplied} pts.`, 'info');

            // Highlight the matching pair
            hint.hintCardIds.forEach(id => {
                const tile = document.querySelector(`.card-tile[data-card-id="${id}"]`);
                if (tile) tile.classList.add('hint-highlight');
            });

            // Update hints counter
            this.session.hintsUsed = hint.hintsUsed;
            document.getElementById('hud-hints').textContent = hint.hintsUsed;

            // Remove highlight after 1.8 seconds
            setTimeout(() => {
                hint.hintCardIds.forEach(id => {
                    const tile = document.querySelector(`.card-tile[data-card-id="${id}"]`);
                    if (tile) tile.classList.remove('hint-highlight');
                });
            }, 1800);

        } catch (err) {
            UI.showToast(err.message, 'error');
        }
    },

    async saveProgress() {
        if (!this.session) return;
        try {
            const saved = await API.saveGame(this.session.sessionId);
            this.session = saved;
            UI.showToast('Progress saved successfully!', 'success');
        } catch (err) {
            UI.showToast('Failed to save progress: ' + err.message, 'error');
        }
    },

    // ── Local Live Timer ──────────────────────────────────────────────────────

    startTimer() {
        this.stopTimer();
        this.timerInterval = setInterval(() => {
            if (this.session && this.session.status === 'ACTIVE') {
                this.session.elapsedSeconds++;
                document.getElementById('hud-timer').textContent = UI.formatTime(this.session.elapsedSeconds);
            }
        }, 1000);
    },

    stopTimer() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
    }
};

// Initialize Application once DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    GameApp.init();
});
