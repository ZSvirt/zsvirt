package org.zstack.core;

public enum StartMode {
    DEFAULT("default"),
    SIMULATOR("simulator"),
    MINIMAL("minimal");

    private String mode;

    StartMode(String mode){
        this.mode = mode;
    }

    @Override
    public String toString() {
        return mode;
    }
}
