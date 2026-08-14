package org.zstack.header.core.encrypt;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import sun.awt.PlatformFont;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @Author: DaoDao
 * @Date: 2021/10/29
 */
@PythonClassInventory
@Inventory(mappingVOClass = EncryptionIntegrityVO.class)
public class EncryptionIntegrityInventory implements Serializable {
    private long id;
    private String resourceUuid;
    private String resourceType;
    private String signedText;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static EncryptionIntegrityInventory __example__() {
        EncryptionIntegrityInventory ret = new EncryptionIntegrityInventory();
        ret.id = 1l;
        ret.resourceType = "ImageVO";
        ret.resourceUuid = "631d2407d5864798a2b27699449ecc29";
        ret.signedText = "631d2407d5864798a2b27699449ecc29";
        return ret;
    }

    public static EncryptionIntegrityInventory valueOf(EncryptionIntegrityVO vo) {
        EncryptionIntegrityInventory inv = new EncryptionIntegrityInventory();
        inv.id = vo.getId();
        inv.resourceUuid = vo.getResourceUuid();
        inv.resourceType = vo.getResourceType();
        inv.signedText = vo.getSignedText();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<EncryptionIntegrityInventory> valueOf(Collection<EncryptionIntegrityVO> vos) {
        return vos.stream().map(EncryptionIntegrityInventory::valueOf).collect(Collectors.toList());
    }
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getSignedText() {
        return signedText;
    }

    public void setSignedText(String signedText) {
        this.signedText = signedText;
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
