package org.zstack.sso.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/2/3
 */
@PythonClassInventory
@Inventory(mappingVOClass = SSOTokenVO.class)
public class SSOTokenInventory implements Serializable {
    private String uuid;
    private String clientUuid;
    private String userUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public SSOTokenInventory(SSOTokenVO vo) {
        this.setUuid(vo.getUuid());
        this.setClientUuid(vo.getClientUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUserUuid(vo.getUserUuid());
    }

    public SSOTokenInventory() {
    }


    public static SSOTokenInventory valueOf(SSOTokenVO vo) {
        return new SSOTokenInventory(vo);
    }

    public static List<SSOTokenInventory> valueOf(Collection<SSOTokenVO> vos) {
        return CollectionUtils.transform(vos, SSOTokenInventory::valueOf);
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
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

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }
}
