package org.zstack.sns;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.sns.platform.email.SNSEmailPlatformFactory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSApplicationPlatformVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "endpoints", inventoryClass = SNSApplicationEndpointInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "platformUuid")
})
public class SNSApplicationPlatformInventory {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SNSApplicationPlatformInventory __example__() {
        SNSApplicationPlatformInventory inv = new SNSApplicationPlatformInventory();
        inv.setUuid(DocUtils.createFixedUuid(SNSApplicationPlatformVO.class));
        inv.setName("email platform");
        inv.setDescription("example description");
        inv.setState(SNSApplicationPlatformState.Enabled.toString());
        inv.setType(SNSEmailPlatformFactory.type.toString());
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        return inv;
    }

    public SNSApplicationPlatformInventory() {
    }

    public SNSApplicationPlatformInventory(SNSApplicationPlatformVO vo) {
        uuid = vo.getUuid();
        name = vo.getName();
        description = vo.getDescription();
        type = vo.getType();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
        state = vo.getState().toString();
    }

    public SNSApplicationPlatformInventory(SNSApplicationPlatformInventory other) {
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
    }

    public static SNSApplicationPlatformInventory valueOf(SNSApplicationPlatformVO vo) {
        return new SNSApplicationPlatformInventory(vo);
    }

    public static List<SNSApplicationPlatformInventory> valueOf(Collection<SNSApplicationPlatformVO> vos) {
        return vos.stream().map(SNSApplicationPlatformInventory::valueOf).collect(Collectors.toList());
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
