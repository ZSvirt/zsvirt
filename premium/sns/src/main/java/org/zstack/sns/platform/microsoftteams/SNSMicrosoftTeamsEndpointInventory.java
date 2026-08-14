package org.zstack.sns.platform.microsoftteams;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSSubscriberInventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSMicrosoftTeamsEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSMicrosoftTeamsEndpointInventory extends SNSApplicationEndpointInventory {
    private String url;

    public static SNSMicrosoftTeamsEndpointInventory __example1__() {
        SNSMicrosoftTeamsEndpointInventory inv = new SNSMicrosoftTeamsEndpointInventory(SNSApplicationEndpointInventory.__example__());
        inv.setUrl("http://teams-robot-url");
        return inv;
    }

    public SNSMicrosoftTeamsEndpointInventory() {
    }

    public SNSMicrosoftTeamsEndpointInventory(SNSMicrosoftTeamsEndpointVO vo) {
        super(vo);
        url = vo.getUrl();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SNSMicrosoftTeamsEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSMicrosoftTeamsEndpointInventory valueOf(SNSMicrosoftTeamsEndpointVO vo) {
        return new SNSMicrosoftTeamsEndpointInventory(vo);
    }

    public static List<SNSMicrosoftTeamsEndpointInventory> valueOf1(Collection<SNSMicrosoftTeamsEndpointVO> vos) {
        return vos.stream().map(SNSMicrosoftTeamsEndpointInventory::valueOf).collect(Collectors.toList());
    }
}
