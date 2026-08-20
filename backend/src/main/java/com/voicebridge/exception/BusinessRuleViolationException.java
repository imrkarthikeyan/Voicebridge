package com.voicebridge.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a request is well-formed but violates a domain rule
 * (e.g. raising a hand twice, joining a closed meeting).
 */
public class BusinessRuleViolationException extends ApiException {

    public BusinessRuleViolationException(String message) {
        super(HttpStatus.CONFLICT, message, "BUSINESS_RULE_VIOLATION");
    }
}
