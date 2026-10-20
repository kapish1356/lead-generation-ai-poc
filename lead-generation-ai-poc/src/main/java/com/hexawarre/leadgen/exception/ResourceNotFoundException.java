package com.hexawarre.leadgen.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String what, Long id) {
        super(what + " not found with id " + id);
    }
}
