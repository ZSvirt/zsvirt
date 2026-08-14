package org.zstack.kvm;

import org.springframework.web.util.UriComponentsBuilder;

public class KVMHostContext {
    private KVMHostInventory inventory;
    private String baseUrl;

    public KVMHostInventory getInventory() {
        return inventory;
    }
    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String buildUrl(String...path) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        for (String p : path) {
            ub.path(p);
        }
        return ub.build().toString();
    }
}
