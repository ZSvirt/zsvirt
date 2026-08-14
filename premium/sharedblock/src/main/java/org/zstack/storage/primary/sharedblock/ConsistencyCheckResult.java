package org.zstack.storage.primary.sharedblock;

public class ConsistencyCheckResult {
    private boolean consistent;
    private boolean sharedBlockGroupFound;

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public boolean isSharedBlockGroupFound() {
        return sharedBlockGroupFound;
    }

    public void setSharedBlockGroupFound(boolean sharedBlockGroupFound) {
        this.sharedBlockGroupFound = sharedBlockGroupFound;
    }
}
