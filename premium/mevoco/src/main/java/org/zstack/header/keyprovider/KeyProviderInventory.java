package org.zstack.header.keyprovider;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = KeyProviderVO.class)
@PythonClassInventory
public class KeyProviderInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private boolean connected;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected KeyProviderInventory(KeyProviderVO vo) {
        setUuid(vo.getUuid());
        setName(vo.getName());
        setDescription(vo.getDescription());
        setType(vo.getType() == null ? null : vo.getType().toString());
        setConnected(vo.isConnected());
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
    }

    public KeyProviderInventory() {
    }

    public static KeyProviderInventory valueOf(KeyProviderVO vo) {
        return new KeyProviderInventory(vo);
    }

    public static List<KeyProviderInventory> valueOf(Collection<KeyProviderVO> vos) {
        return CollectionUtils.transform(vos, KeyProviderInventory::valueOf);
    }

    public static KeyProviderInventory __example__() {
        KeyProviderInventory inv = new KeyProviderInventory();
        inv.setUuid(DocUtils.createFixedUuid(KeyProviderVO.class));
        inv.setName("kms-1");
        inv.setDescription("example");
        inv.setType(KeyProviderType.KMS.toString());
        inv.setConnected(true);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
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
