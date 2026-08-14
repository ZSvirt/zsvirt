package org.zstack.sns.platform.feishu;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSSubscriberInventory;

import javax.persistence.JoinColumn;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSFeiShuEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSFeiShuEndpointInventory extends SNSApplicationEndpointInventory {
    private String url;
    private boolean atAll;
    @Queryable(mappingClass = SNSFeiShuAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid", referencedColumnName = "userId"))
    private List<String> atPersonUserIds;
    @Queryable(mappingClass = SNSFeiShuAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid"))
    private List<SNSFeiShuAtPersonInventory> atPersonList;

    private String secret;

    public static SNSFeiShuEndpointInventory __example1__() {
        SNSFeiShuEndpointInventory inv = new SNSFeiShuEndpointInventory(SNSApplicationEndpointInventory.__example__());
        inv.setUrl("http://feishu-url");
        return inv;
    }

    public SNSFeiShuEndpointInventory() {
    }

    public SNSFeiShuEndpointInventory(SNSFeiShuEndpointVO vo) {
        super(vo);
        url = vo.getUrl();
        atAll = vo.isAtAll();
        atPersonUserIds = vo.getAtPersons().stream().map(SNSFeiShuAtPersonVO::getUserId).collect(Collectors.toList());
        atPersonList = vo.getAtPersons().stream().map(SNSFeiShuAtPersonInventory::valueOf).collect(Collectors.toList());
        secret = vo.getSecret();
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

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<SNSFeiShuAtPersonInventory> getAtPersonList() {
        return atPersonList;
    }

    public void setAtPersonList(List<SNSFeiShuAtPersonInventory> atPersonList) {
        this.atPersonList = atPersonList;
    }

    public SNSFeiShuEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSFeiShuEndpointInventory valueOf(SNSFeiShuEndpointVO vo) {
        return new SNSFeiShuEndpointInventory(vo);
    }

    public static List<SNSFeiShuEndpointInventory> valueOf1(Collection<SNSFeiShuEndpointVO> vos) {
        return vos.stream().map(SNSFeiShuEndpointInventory::valueOf).collect(Collectors.toList());
    }
}
