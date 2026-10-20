package com.hexawarre.leadgen.exception;

public class DuplicateLeadException extends RuntimeException {

    public DuplicateLeadException(String email) {
        super("A lead with email '" + email + "' already exists");
    }
}
