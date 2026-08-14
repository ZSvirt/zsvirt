package org.zstack.header.rest;

public class RestException extends Exception {
    public int statusCode;
    public String error;

    public RestException(int statusCode, String error) {
        this.statusCode = statusCode;
        this.error = error;
    }
}
