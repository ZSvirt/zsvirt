package org.zstack.header.keyprovider;

import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = EncryptedResourceKeyRefVO.class)
public class EncryptedResourceKeyRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String resourceType;
    private String resourceUuid;
    private String providerUuid;
    private String providerName;
    private Integer keyVersion;
    private String kekRef;
    @NoLogging
    private String wrappedDek;
    private String algorithm;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static EncryptedResourceKeyRefInventory valueOf(EncryptedResourceKeyRefVO vo) {
        EncryptedResourceKeyRefInventory inv = new EncryptedResourceKeyRefInventory();
        inv.setId(vo.getId());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setProviderUuid(vo.getProviderUuid());
        inv.setProviderName(vo.getProviderName());
        inv.setKeyVersion(vo.getKeyVersion());
        inv.setKekRef(vo.getKekRef());
        inv.setWrappedDek(vo.getWrappedDek());
        inv.setAlgorithm(vo.getAlgorithm());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<EncryptedResourceKeyRefInventory> valueOf(Collection<EncryptedResourceKeyRefVO> vos) {
        return vos.stream().map(EncryptedResourceKeyRefInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public String getKekRef() {
        return kekRef;
    }

    public void setKekRef(String kekRef) {
        this.kekRef = kekRef;
    }

    public String getWrappedDek() {
        return wrappedDek;
    }

    public void setWrappedDek(String wrappedDek) {
        this.wrappedDek = wrappedDek;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
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
