/**
 * audio.js - Audio & Sound Effects Manager
 * Supports HTML5 <audio> elements for custom sound files with an integrated
 * Web Audio API synthesizer fallback so sound effects & melodies work out-of-the-box!
 */

const AudioManager = {
    soundEnabled: true,
    musicEnabled: false,
    musicVolume: 0.3,
    audioContext: null,

    // HTML5 Audio elements
    audioElements: {
        flip: null,
        match: null,
        nomatch: null,
        win: null,
        bgmusic: null
    },

    init() {
        // Initialize HTML5 audio elements if available
        this.audioElements.flip = new Audio('assets/audio/flip.mp3');
        this.audioElements.match = new Audio('assets/audio/match.mp3');
        this.audioElements.nomatch = new Audio('assets/audio/nomatch.mp3');
        this.audioElements.win = new Audio('assets/audio/win.mp3');
        this.audioElements.bgmusic = new Audio('assets/audio/bgmusic.mp3');

        if (this.audioElements.bgmusic) {
            this.audioElements.bgmusic.loop = true;
            this.audioElements.bgmusic.volume = this.musicVolume;
        }

        // Restore sound settings from localStorage if available
        const savedSound = localStorage.getItem('memoryGame_sound');
        if (savedSound !== null) this.soundEnabled = savedSound === 'true';

        const savedMusic = localStorage.getItem('memoryGame_music');
        if (savedMusic !== null) this.musicEnabled = savedMusic === 'true';
    },

    getAudioContext() {
        if (!this.audioContext) {
            const AudioContextClass = window.AudioContext || window.webkitAudioContext;
            if (AudioContextClass) {
                this.audioContext = new AudioContextClass();
            }
        }
        if (this.audioContext && this.audioContext.state === 'suspended') {
            this.audioContext.resume();
        }
        return this.audioContext;
    },

    toggleSound() {
        this.soundEnabled = !this.soundEnabled;
        localStorage.setItem('memoryGame_sound', this.soundEnabled);
        return this.soundEnabled;
    },

    toggleMusic() {
        this.musicEnabled = !this.musicEnabled;
        localStorage.setItem('memoryGame_music', this.musicEnabled);
        if (this.musicEnabled) {
            this.playMusic();
        } else {
            this.stopMusic();
        }
        return this.musicEnabled;
    },

    playMusic() {
        if (!this.musicEnabled) return;
        const bg = this.audioElements.bgmusic;
        if (bg) {
            bg.volume = this.musicVolume;
            bg.play().catch(() => {
                // Background music file not present or autoplay blocked; synthesized ambient chord fallback
                this.playSynthAmbient();
            });
        }
    },

    stopMusic() {
        const bg = this.audioElements.bgmusic;
        if (bg) {
            bg.pause();
            bg.currentTime = 0;
        }
        if (this.ambientInterval) {
            clearInterval(this.ambientInterval);
            this.ambientInterval = null;
        }
    },

    playFlip() {
        if (!this.soundEnabled) return;
        this.playWithFallback(this.audioElements.flip, () => {
            // High-pitched card tap/whoosh
            const ctx = this.getAudioContext();
            if (!ctx) return;
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(440, ctx.currentTime);
            osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.08);
            gain.gain.setValueAtTime(0.15, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start();
            osc.stop(ctx.currentTime + 0.08);
        });
    },

    playMatch() {
        if (!this.soundEnabled) return;
        this.playWithFallback(this.audioElements.match, () => {
            // Bright cheerful chime (C5 -> E5 -> G5)
            const ctx = this.getAudioContext();
            if (!ctx) return;
            [523.25, 659.25, 783.99].forEach((freq, idx) => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.09);
                gain.gain.setValueAtTime(0.2, ctx.currentTime + idx * 0.09);
                gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + idx * 0.09 + 0.25);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(ctx.currentTime + idx * 0.09);
                osc.stop(ctx.currentTime + idx * 0.09 + 0.25);
            });
        });
    },

    playNoMatch() {
        if (!this.soundEnabled) return;
        this.playWithFallback(this.audioElements.nomatch, () => {
            // Gentle low double thud
            const ctx = this.getAudioContext();
            if (!ctx) return;
            [220, 180].forEach((freq, idx) => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'sawtooth';
                osc.frequency.setValueAtTime(freq, ctx.currentTime + idx * 0.12);
                gain.gain.setValueAtTime(0.12, ctx.currentTime + idx * 0.12);
                gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + idx * 0.12 + 0.15);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(ctx.currentTime + idx * 0.12);
                osc.stop(ctx.currentTime + idx * 0.12 + 0.15);
            });
        });
    },

    playWin() {
        if (!this.soundEnabled) return;
        this.playWithFallback(this.audioElements.win, () => {
            // Victory fanfare
            const ctx = this.getAudioContext();
            if (!ctx) return;
            const notes = [
                { f: 523.25, t: 0.0,  d: 0.12 }, // C5
                { f: 659.25, t: 0.12, d: 0.12 }, // E5
                { f: 783.99, t: 0.24, d: 0.12 }, // G5
                { f: 1046.5, t: 0.36, d: 0.40 }  // C6
            ];
            notes.forEach(n => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'triangle';
                osc.frequency.setValueAtTime(n.f, ctx.currentTime + n.t);
                gain.gain.setValueAtTime(0.25, ctx.currentTime + n.t);
                gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + n.t + n.d);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(ctx.currentTime + n.t);
                osc.stop(ctx.currentTime + n.t + n.d);
            });
        });
    },

    playWithFallback(audioEl, fallbackFn) {
        if (audioEl && audioEl.readyState >= 2) {
            audioEl.currentTime = 0;
            audioEl.play().catch(() => fallbackFn());
        } else {
            fallbackFn();
        }
    },

    playSynthAmbient() {
        if (this.ambientInterval) return;
        const ctx = this.getAudioContext();
        if (!ctx) return;

        const chords = [
            [261.63, 329.63, 392.00], // C
            [220.00, 261.63, 329.63], // Am
            [174.61, 220.00, 261.63], // F
            [196.00, 246.94, 293.66]  // G
        ];
        let idx = 0;

        const playChord = () => {
            if (!this.musicEnabled) return;
            const chord = chords[idx % chords.length];
            idx++;
            chord.forEach(freq => {
                const osc = ctx.createOscillator();
                const gain = ctx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, ctx.currentTime);
                gain.gain.setValueAtTime(0.015, ctx.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 3.8);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start();
                osc.stop(ctx.currentTime + 3.8);
            });
        };

        playChord();
        this.ambientInterval = setInterval(playChord, 4000);
    }
};

window.AudioManager = AudioManager;
