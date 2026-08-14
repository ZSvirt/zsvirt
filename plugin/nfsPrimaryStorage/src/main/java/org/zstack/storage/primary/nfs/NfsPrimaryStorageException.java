package org.zstack.storage.primary.nfs;

public class NfsPrimaryStorageException extends Exception {
    public NfsPrimaryStorageException(String msg) {
        super(msg);
    }
    
    public NfsPrimaryStorageException(String msg, Throwable t) {
        super(msg, t);
    }
}
