package org.zstack.sns;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.sns.platform.email.SNSEmailPlatformFactory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSApplicationEndpointVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSApplicationEndpointInventory {
    private String name;
    private String uuid;
    private String description;
    private String type;
    private String state;
    private String platformUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String connectionStatus;
    @Unqueryable
    private SNSApplicationPlatformInventory platform;

    public static SNSApplicationEndpointInventory __example__() {
        SNSApplicationEndpointInventory inv = new SNSApplicationEndpointInventory();
        inv.setName("example");
        inv.setUuid("917fe66f76b241f0843dfbd6be685fc1");
        inv.setDescription("description example");
        inv.setType(SNSEmailPlatformFactory.type.toString());
        inv.setPlatformUuid("4eb0ceebf95b452585ebeacd647d9f35");
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        inv.setState(SNSApplicationEndpointState.Enabled.toString());
        inv.setConnectionStatus(SNSApplicationEndpointConnectionStatus.UP.toString());
        return inv;
    }

    public SNSApplicationEndpointInventory() {
    }

    public SNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        name = vo.getName();
        uuid = vo.getUuid();
        description = vo.getDescription();
        type = vo.getType();
        platformUuid = vo.getPlatformUuid();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
        state = vo.getState().toString();
        platform = vo.getPlatform().toInventory();
        connectionStatus = vo.getConnectionStatus();
    }


    public SNSApplicationEndpointInventory(SNSApplicationEndpointInventory other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.type = other.type;
        this.state = other.state;
        this.platformUuid = other.platformUuid;
        this.platform = other.platform;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.connectionStatus = other.connectionStatus;
    }

    public static SNSApplicationEndpointInventory valueOf(SNSApplicationEndpointVO vo) {
        return new SNSApplicationEndpointInventory(vo);
    }

    public static List<SNSApplicationEndpointInventory> valueOf(Collection<SNSApplicationEndpointVO> vos) {
        return vos.stream().map(SNSApplicationEndpointInventory::valueOf).collect(Collectors.toList());
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

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    public SNSApplicationPlatformInventory getPlatform() {
        return platform;
    }

    public void setPlatform(SNSApplicationPlatformInventory platform) {
        this.platform = platform;
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

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }
}
