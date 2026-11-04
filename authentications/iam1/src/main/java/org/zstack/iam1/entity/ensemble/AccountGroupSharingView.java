package org.zstack.iam1.entity.ensemble;

import org.zstack.header.message.DocUtils;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
public class AccountGroupSharingView {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static AccountGroupSharingView __example__() {
        AccountGroupSharingView inventory = new AccountGroupSharingView();
        inventory.setUuid(DocUtils.createFixedUuid(AccountGroupVO.class));
        return inventory;
    }
}
