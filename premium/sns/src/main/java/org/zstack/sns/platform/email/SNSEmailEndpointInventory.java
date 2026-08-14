package org.zstack.sns.platform.email;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.sns.*;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSEmailEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSEmailPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSEmailEndpointInventory extends SNSApplicationEndpointInventory {
    private String email;

    @Queryable(mappingClass = SNSEmailAddressInventory.class,
            joinColumn = @JoinColumn(name="endpointUuid"))
    private List<SNSEmailAddressInventory> emailAddresses;

    public SNSEmailEndpointInventory() {
    }

    public SNSEmailEndpointInventory(SNSEmailEndpointVO vo) {
        super(vo);
    }

    public SNSEmailEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSEmailEndpointInventory valueOf(SNSEmailEndpointVO vo) {
        SNSEmailEndpointInventory inv = new SNSEmailEndpointInventory(vo);
        inv.email = vo.getEmail();
        inv.emailAddresses = vo.getEmailAddresses().stream().map(SNSEmailAddressInventory::valueOf).collect(Collectors.toList());
        return inv;
    }

    public static List<SNSEmailEndpointInventory> valueOf1(Collection<SNSEmailEndpointVO> vos) {
        return vos.stream().map(SNSEmailEndpointInventory::valueOf).collect(Collectors.toList());
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<SNSEmailAddressInventory> getEmailAddresses() {
        return emailAddresses;
    }

    public void setEmailAddresses(List<SNSEmailAddressInventory> emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    public static SNSEmailEndpointInventory __example1__() {
        SNSEmailEndpointInventory inv = new SNSEmailEndpointInventory();
        inv.setName("example");
        inv.setUuid("917fe66f76b241f0843dfbd6be685fc1");
        inv.setDescription("description example");
        inv.setType(SNSEmailPlatformFactory.type.toString());
        inv.setPlatformUuid("4eb0ceebf95b452585ebeacd647d9f35");
        inv.setCreateDate(new Timestamp(System.currentTimeMillis()));
        inv.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        inv.setState(SNSApplicationEndpointState.Enabled.toString());
        inv.setEmail("example@zstack.io");
        return inv;
    }
}
