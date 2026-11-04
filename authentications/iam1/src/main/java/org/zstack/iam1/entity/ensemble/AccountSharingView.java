package org.zstack.iam1.entity.ensemble;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.DocUtils;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
public class AccountSharingView {
    private String uuid;

    /**
     * @see AccountInventory#getUuid()
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @see AccountInventory#setUuid(String)
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static AccountSharingView __example__() {
        AccountSharingView inventory = new AccountSharingView();
        inventory.setUuid(DocUtils.createFixedUuid(AccountVO.class));
        return inventory;
    }
}
