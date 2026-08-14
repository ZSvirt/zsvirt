package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;

import java.io.Serializable;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class VmAccountPreference implements Serializable, Cloneable {
    private String userAccount;
    @NoLogging
    private String accountPassword;
    private String vmUuid;


    public VmAccountPreference() {

    }

    public VmAccountPreference(String vmUuid, String userAccount, String accountPassword) {
        this.userAccount = userAccount;
        this.accountPassword = accountPassword;
        this.vmUuid = vmUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }
}
