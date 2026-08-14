package org.zstack.accessKey;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccessKeyVO.class)
@PythonClassInventory
public class AccessKeyInventory {
    private String uuid;
    private String description;
    private String accountUuid;
    @Deprecated
    private String userUuid;
    private String AccessKeyID;
    @GsonTransient(transientInHttpResponse = false)
    private String AccessKeySecret;
    private AccessKeyState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccessKeyInventory valueOf(AccessKeyVO vo) {
        AccessKeyInventory inv = new AccessKeyInventory();
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setUserUuid(vo.getAccountUuid());
        inv.setAccessKeyID(vo.getAccessKeyID());
        inv.setAccessKeySecret(vo.getAccessKeySecret());
        inv.setState(vo.getState());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccessKeyInventory> valueOf(Collection<AccessKeyVO> vos) {
        return CollectionUtils.transform(vos, AccessKeyInventory::valueOf);
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Deprecated
    public String getUserUuid() {
        return userUuid;
    }

    @Deprecated
    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public AccessKeyState getState() {
        return state;
    }

    public void setState(AccessKeyState state) {
        this.state = state;
    }

    public String getAccessKeyID() {
        return AccessKeyID;
    }

    public void setAccessKeyID(String accessKeyID) {
        AccessKeyID = accessKeyID;
    }

    public String getAccessKeySecret() {
        return AccessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        AccessKeySecret = accessKeySecret;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public static AccessKeyInventory __example__() {
        AccessKeyInventory inventory = new AccessKeyInventory();
        inventory.setUuid(StringDSL.createFixedUuid(AccessKeyVO.class));
        inventory.setAccountUuid(StringDSL.createFixedUuid(AccountVO.class));
        inventory.setUserUuid(inventory.getAccountUuid());
        inventory.setState(AccessKeyState.Enabled);
        inventory.setAccessKeyID("1234567890abcdedfhij");
        inventory.setAccessKeySecret("1234567890abcdedfhij1234567890abcdedfhij");
        return inventory;
    }
}
