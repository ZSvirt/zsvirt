package org.zstack.identity.imports.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/06/12
 */
@Inventory(mappingVOClass = AccountThirdPartyAccountSourceRefVO.class)
@PythonClassInventory
public class AccountThirdPartyAccountSourceRefInventory {
    private Long id;
    private String credentials;
    private String accountSourceUuid;
    private String accountUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountThirdPartyAccountSourceRefInventory valueOf(AccountThirdPartyAccountSourceRefVO vo) {
        AccountThirdPartyAccountSourceRefInventory inv = new AccountThirdPartyAccountSourceRefInventory();
        inv.setId(vo.getId());
        inv.setCredentials(vo.getCredentials());
        inv.setAccountSourceUuid(vo.getAccountSourceUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountThirdPartyAccountSourceRefInventory> valueOf(Collection<AccountThirdPartyAccountSourceRefVO> vos) {
        return CollectionUtils.transform(vos, AccountThirdPartyAccountSourceRefInventory::valueOf);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getAccountSourceUuid() {
        return accountSourceUuid;
    }

    public void setAccountSourceUuid(String accountSourceUuid) {
        this.accountSourceUuid = accountSourceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
