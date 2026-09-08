/**
 * ui.js - Screen management, modals, toasts, and UI utilities.
 */

const UI = {
    currentScreen: null,

    init() {
        this.updateAudioControls();
    },

    showScreen(screenId) {
        document.querySelectorAll('.screen').forEach(el => el.classList.remove('active'));
        const target = document.getElementById(`${screenId}-screen`);
        if (target) {
            target.classList.add('active');
            this.currentScreen = screenId;
        }
    },

    updateUserBadge(user) {
        const badge = document.getElementById('user-badge-container');
        if (!badge) return;
        if (user) {
            badge.style.display = 'flex';
            document.getElementById('user-display-name').textContent = user.displayName || user.username;
        } else {
            badge.style.display = 'none';
        }
    },

    updateAudioControls() {
        const soundBtn = document.getElementById('btn-toggle-sound');
        const musicBtn = document.getElementById('btn-toggle-music');

        if (soundBtn) {
            soundBtn.textContent = AudioManager.soundEnabled ? '🔊 Sound: ON' : '🔇 Sound: OFF';
            soundBtn.classList.toggle('btn-secondary', !AudioManager.soundEnabled);
            soundBtn.classList.toggle('btn-accent', AudioManager.soundEnabled);
        }

        if (musicBtn) {
            musicBtn.textContent = AudioManager.musicEnabled ? '🎵 Music: ON' : '🔇 Music: OFF';
            musicBtn.classList.toggle('btn-secondary', !AudioManager.musicEnabled);
            musicBtn.classList.toggle('btn-accent', AudioManager.musicEnabled);
        }
    },

    showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        
        let icon = 'ℹ️';
        if (type === 'success') icon = '✅';
        if (type === 'error') icon = '❌';

        toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    },

    showWinModal({ score, moves, elapsedSeconds, difficulty, hintsUsed }) {
        const modal = document.getElementById('win-modal');
        if (!modal) return;

        document.getElementById('win-stat-score').textContent = score;
        document.getElementById('win-stat-moves').textContent = moves;
        document.getElementById('win-stat-time').textContent = this.formatTime(elapsedSeconds);
        document.getElementById('win-stat-hints').textContent = hintsUsed;
        document.getElementById('win-stat-diff').textContent = difficulty;

        modal.classList.add('active');
    },

    hideWinModal() {
        const modal = document.getElementById('win-modal');
        if (modal) modal.classList.remove('active');
    },

    formatTime(totalSeconds) {
        const mins = Math.floor(totalSeconds / 60);
        const secs = Math.floor(totalSeconds % 60);
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
};

window.UI = UI;
