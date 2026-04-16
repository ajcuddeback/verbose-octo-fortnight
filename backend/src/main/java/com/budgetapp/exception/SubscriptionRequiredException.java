package com.budgetapp.exception;

public class SubscriptionRequiredException extends RuntimeException {

    public SubscriptionRequiredException() {
        super("An active subscription is required to access this feature.");
    }

    public SubscriptionRequiredException(String message) {
        super(message);
    }
}
