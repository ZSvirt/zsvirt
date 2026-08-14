package org.zstack.sns;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2019-07-10
 */
@Inventory(mappingVOClass = SNSSmsEndpointVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSSmsEndpointInventory extends SNSApplicationEndpointInventory {
    @Queryable(mappingClass = SNSSmsReceiverInventory.class,
            joinColumn = @JoinColumn(name = "endpointUuid"))
    private List<SNSSmsReceiverInventory> receivers;

    public SNSSmsEndpointInventory() {
    }

    public SNSSmsEndpointInventory(SNSSmsEndpointVO vo) {
        super(vo);
        receivers = vo.getReceivers().stream().map(SNSSmsReceiverInventory::valueOf).collect(Collectors.toList());
    }

    public SNSSmsEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSSmsEndpointInventory __example__() {
        SNSSmsEndpointInventory inventory = new SNSSmsEndpointInventory();
        inventory.setName("example");
        inventory.setUuid("041723a4d9d14e8686dbec7d1369c05e");
        inventory.setDescription("example SNSSmsEndpoint Description");
        inventory.setType(SNSConstants.ALIYUNSMS_PLATFORM);
        inventory.setPlatformUuid("4eb0ceebf95b452585ebeacd647d9f35");
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        inventory.setState(SNSApplicationEndpointState.Enabled.toString());
        return inventory;
    }

    public static SNSSmsEndpointInventory valueOf(SNSSmsEndpointVO vo) {
        return new SNSSmsEndpointInventory(vo);
    }

    public static List<SNSSmsEndpointInventory> valueOf1(Collection<SNSSmsEndpointVO> vos) {
        return vos.stream().map(SNSSmsEndpointInventory::valueOf).collect(Collectors.toList());
    }

    public List<SNSSmsReceiverInventory> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<SNSSmsReceiverInventory> receivers) {
        this.receivers = receivers;
    }
}
