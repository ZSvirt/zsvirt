package org.zstack.kvm;


public class KVMHostConnectException extends Exception {
    public KVMHostConnectException(String error) {
        super(error);
    }
    
    public KVMHostConnectException(String error, Throwable t) {
        super(error, t);
    }
} 
