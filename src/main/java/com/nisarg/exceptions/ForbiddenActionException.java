package com.nisarg.exceptions;

/*
 Use when authenticated user tries an action they are not allowed to perform.

 Examples:
 - User tries cancelling another user's booking
 - User tries modifying another user's resource
 - Role-based access denial
*/
public class ForbiddenActionException extends RuntimeException {

    public ForbiddenActionException(String message) {
        super(message);
    }
}