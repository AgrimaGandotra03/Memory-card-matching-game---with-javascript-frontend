package com.memorygame.dto;

/**
 * Generic error response returned whenever something goes wrong.
 * Every error from our API looks like: { "error": "Some message here" }
 */
public class ErrorResponse {

    private String error;

    public ErrorResponse() {}

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
