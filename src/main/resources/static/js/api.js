/**
 * api.js - Centralized API Service for Memory Card Matching Game
 * Handles all REST requests to the Spring Boot backend with error handling.
 */

const API_BASE_URL = window.location.origin.includes(":8080")
  ? ""
  : "http://localhost:8080";

const API = {
  /**
   * Generic fetch helper with JSON parsing and standardized error handling.
   */
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const defaultHeaders = {
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    try {
      const response = await fetch(url, {
        ...options,
        headers: {
          ...defaultHeaders,
          ...options.headers,
        },
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMessage =
          data && (data.error || data.message)
            ? data.error || data.message
            : `HTTP Error ${response.status}: ${response.statusText}`;
        throw new Error(errorMessage);
      }

      return data;
    } catch (error) {
      console.error(
        `[API Error] ${options.method || "GET"} ${endpoint}:`,
        error.message,
      );
      throw error;
    }
  },

  // ── Auth Endpoints ────────────────────────────────────────────────────────

  async register(username, password, displayName) {
    return this.request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        username,
        password,
        displayName: displayName || username,
      }),
    });
  },

  async login(username, password) {
    return this.request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
  },

  // ── Profile Endpoints ─────────────────────────────────────────────────────

  async getProfile(userId) {
    return this.request(`/api/profile/${userId}`);
  },

  async updateProfile(userId, { displayName, preferredTheme }) {
    return this.request(`/api/profile/${userId}`, {
      method: "PUT",
      body: JSON.stringify({ displayName, preferredTheme }),
    });
  },

  // ── Game Engine Endpoints ─────────────────────────────────────────────────

  async startGame(
    userId,
    difficulty,
    theme,
    focusMode = false,
    mode = "CLASSIC",
    dailyChallenge = false,
    dailyChallengeDate = null,
  ) {
    return this.request("/api/game/start", {
      method: "POST",
      body: JSON.stringify({
        userId,
        difficulty,
        theme,
        focusMode,
        mode,
        dailyChallenge,
        dailyChallengeDate,
      }),
    });
  },

  async flipCard(sessionId, cardId) {
    return this.request(`/api/game/${sessionId}/flip`, {
      method: "POST",
      body: JSON.stringify({ cardId }),
    });
  },

  async pauseGame(sessionId) {
    return this.request(`/api/game/${sessionId}/pause`, {
      method: "POST",
    });
  },

  async resumeGame(sessionId) {
    return this.request(`/api/game/${sessionId}/resume`, {
      method: "POST",
    });
  },

  async restartGame(sessionId) {
    return this.request(`/api/game/${sessionId}/restart`, {
      method: "POST",
    });
  },

  async getHint(sessionId) {
    return this.request(`/api/game/${sessionId}/hint`);
  },

  async saveGame(sessionId) {
    return this.request(`/api/game/${sessionId}/save`, {
      method: "POST",
    });
  },

  async loadGame(userId) {
    return this.request(`/api/game/load?userId=${userId}`);
  },

  async getSession(sessionId) {
    return this.request(`/api/game/${sessionId}`);
  },

  // ── Score History Endpoints ───────────────────────────────────────────────

  async getScoreHistory(userId) {
    return this.request(`/api/scores/${userId}`);
  },

  async getPerformanceHistory(userId) {
    return this.request(`/api/performance/${userId}`);
  },

  async getDailyChallenge(userId) {
    return this.request(`/api/engagement/daily/${userId}`);
  },

  async getBadges(userId) {
    return this.request(`/api/engagement/badges/${userId}`);
  },

  async getRecommendations(userId) {
    return this.request(`/api/engagement/recommendations/${userId}`);
  },
};

window.API = API;
