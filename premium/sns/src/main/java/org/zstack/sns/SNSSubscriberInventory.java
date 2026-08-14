package org.zstack.sns;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSSubscriberVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "endpoints", inventoryClass = SNSApplicationEndpointInventory.class,
        foreignKey = "endpointUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topics", inventoryClass = SNSTopicInventory.class,
                foreignKey = "topicUuid", expandedInventoryKey = "uuid"),
})
public class SNSSubscriberInventory {
    private String topicUuid;
    private String endpointUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SNSSubscriberInventory __example__() {
        SNSSubscriberInventory ret = new SNSSubscriberInventory();
        ret.topicUuid = "3677dc0f00964b80886f3b2bbf9338cd";
        ret.endpointUuid = "f8c5c56233844126ad5f5a0174da3098";
        return ret;
    }

    public static SNSSubscriberInventory valueOf(SNSSubscriberVO vo) {
        SNSSubscriberInventory inv = new SNSSubscriberInventory();
        inv.topicUuid = vo.getTopicUuid();
        inv.endpointUuid = vo.getEndpointUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<SNSSubscriberInventory> valueOf(Collection<SNSSubscriberVO> vos) {
        return vos.stream().map(SNSSubscriberInventory::valueOf).collect(Collectors.toList());
    }

    public String getTopicUuid() {
        return topicUuid;
    }

    public void setTopicUuid(String topicUuid) {
        this.topicUuid = topicUuid;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
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
