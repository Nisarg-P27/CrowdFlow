package com.nisarg.exceptions;

/*
 Use when request is valid but conflicts with current system state.

 Examples:
 - Not enough seats available
 - Booking already cancelled
 - Seat availability changed due to concurrent booking
 - Invalid booking state transition
*/
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}