package org.zstack.header.allocator;

import java.util.Map;

public class AllocateHostSpec {
    private Map<String, Object> userData;
    private AllocateHostMsg allocateMsg;

    public Map<String, Object> getUserData() {
        return userData;
    }

    public void setUserData(Map<String, Object> userData) {
        this.userData = userData;
    }

    public AllocateHostMsg getAllocateMsg() {
        return allocateMsg;
    }

    public void setAllocateMsg(AllocateHostMsg allocateMsg) {
        this.allocateMsg = allocateMsg;
    }
}
