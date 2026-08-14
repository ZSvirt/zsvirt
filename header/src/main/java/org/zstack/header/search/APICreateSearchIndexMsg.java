package org.zstack.header.search;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

public class APICreateSearchIndexMsg extends APIMessage {
    @APIParam
    private List<String> inventoryNames;
    private boolean isRecreate;

    public List<String> getInventoryNames() {
        return inventoryNames;
    }

    public void setInventoryNames(List<String> inventoryNames) {
        this.inventoryNames = inventoryNames;
    }

    public boolean isRecreate() {
        return isRecreate;
    }

    public void setRecreate(boolean isRecreate) {
        this.isRecreate = isRecreate;
    }
 
    public static APICreateSearchIndexMsg __example__() {
        APICreateSearchIndexMsg msg = new APICreateSearchIndexMsg();


        return msg;
    }

}
