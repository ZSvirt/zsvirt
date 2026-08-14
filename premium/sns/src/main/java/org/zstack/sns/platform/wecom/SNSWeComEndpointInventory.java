package org.zstack.sns.platform.wecom;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSSubscriberInventory;

import javax.persistence.JoinColumn;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSWeComEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSWeComEndpointInventory extends SNSApplicationEndpointInventory {
    private String url;
    private boolean atAll;
    @Queryable(mappingClass = SNSWeComAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid", referencedColumnName = "userId"))
    private List<String> atPersonUserIds;
    @Queryable(mappingClass = SNSWeComAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid"))
    private List<SNSWeComAtPersonInventory> atPersonList;

    public static SNSWeComEndpointInventory __example1__() {
        SNSWeComEndpointInventory inv = new SNSWeComEndpointInventory(SNSApplicationEndpointInventory.__example__());
        inv.setUrl("http://wecom-url");
        return inv;
    }

    public SNSWeComEndpointInventory() {
    }

    public SNSWeComEndpointInventory(SNSWeComEndpointVO vo) {
        super(vo);
        url = vo.getUrl();
        atAll = vo.isAtAll();
        atPersonUserIds = vo.getAtPersons().stream().map(SNSWeComAtPersonVO::getUserId).collect(Collectors.toList());
        atPersonList = vo.getAtPersons().stream().map(SNSWeComAtPersonInventory::valueOf).collect(Collectors.toList());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isAtAll() {
        return atAll;
    }

    public void setAtAll(boolean atAll) {
        this.atAll = atAll;
    }

    public List<String> getAtPersonUserIds() {
        return atPersonUserIds;
    }

    public void setAtPersonUserIds(List<String> atPersonUserIds) {
        this.atPersonUserIds = atPersonUserIds;
    }

    public List<SNSWeComAtPersonInventory> getAtPersonList() {
        return atPersonList;
    }

    public void setAtPersonList(List<SNSWeComAtPersonInventory> atPersonList) {
        this.atPersonList = atPersonList;
    }

    public SNSWeComEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSWeComEndpointInventory valueOf(SNSWeComEndpointVO vo) {
        return new SNSWeComEndpointInventory(vo);
    }

    public static List<SNSWeComEndpointInventory> valueOf1(Collection<SNSWeComEndpointVO> vos) {
        return vos.stream().map(SNSWeComEndpointInventory::valueOf).collect(Collectors.toList());
    }
}
