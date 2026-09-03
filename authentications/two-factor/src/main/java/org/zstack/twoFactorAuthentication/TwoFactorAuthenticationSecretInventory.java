package org.zstack.twoFactorAuthentication;

import org.zstack.header.message.GsonTransient;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 *  * Created by shixin on 06/28/2018.
 *   */
@Inventory(mappingVOClass = TwoFactorAuthenticationSecretVO.class)
public class TwoFactorAuthenticationSecretInventory {
    private String uuid;
    @GsonTransient(transientInHttpResponse = false)
    private String secret;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String accountUuid;

    public static TwoFactorAuthenticationSecretInventory valueOf(TwoFactorAuthenticationSecretVO vo) {
        TwoFactorAuthenticationSecretInventory inv = new TwoFactorAuthenticationSecretInventory();
        inv.setUuid(vo.getUuid());
        inv.setSecret(vo.getSecret());
        inv.setStatus(vo.getStatus().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setAccountUuid(vo.getAccountUuid());
        return inv;
    }

    public static List<TwoFactorAuthenticationSecretInventory> valueOf(Collection<TwoFactorAuthenticationSecretVO> vos) {
        return CollectionUtils.transform(vos, TwoFactorAuthenticationSecretInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
