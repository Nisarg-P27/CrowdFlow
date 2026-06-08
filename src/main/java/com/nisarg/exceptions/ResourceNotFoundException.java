package com.nisarg.exceptions;

/*
 Use when requested resource does not exist.

 Examples:
 - Event not found
 - Booking not found
 - User not found
 - Payment not found
*/
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}