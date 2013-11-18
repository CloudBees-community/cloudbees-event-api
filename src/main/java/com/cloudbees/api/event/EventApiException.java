package com.cloudbees.api.event;

/**
 * @author Vivek Pandey
 */
public class EventApiException extends Exception {
    public EventApiException(String message) {
        super(message);
    }

    public EventApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
