package org.zstack.sns.platform.dingtalk;

import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSSubscriberInventory;

import javax.persistence.JoinColumn;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Inventory(mappingVOClass = SNSDingTalkEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSDingTalkEndpointInventory extends SNSApplicationEndpointInventory {
    private String url;
    private boolean atAll;
    private String secret;
    @Queryable(mappingClass = SNSDingTalkAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid", referencedColumnName = "phoneNumber"))
    private List<String> atPersonPhoneNumbers;
    @Queryable(mappingClass = SNSDingTalkAtPersonInventory.class,
            joinColumn = @JoinColumn(name="endpoingUuid"))
    private List<SNSDingTalkAtPersonInventory> atPersonList;

    public static SNSDingTalkEndpointInventory __example1__() {
        SNSDingTalkEndpointInventory inv = new SNSDingTalkEndpointInventory(SNSApplicationEndpointInventory.__example__());
        inv.setUrl("http://dingding-url");
        inv.setAtAll(false);
        inv.setSecret("SECca7c224f47ab16fbe51050ae0b8ebfc505b2b866fc0eb3768c8d79527d1bacc0");
        inv.setAtPersonPhoneNumbers(asList("18900001111"));
        return inv;
    }

    public SNSDingTalkEndpointInventory() {
    }

    public SNSDingTalkEndpointInventory(SNSDingTalkEndpointVO vo) {
        super(vo);
        url = vo.getUrl();
        atAll = vo.isAtAll();
        atPersonPhoneNumbers = vo.getAtPersons().stream().map(SNSDingTalkAtPersonVO::getPhoneNumber).collect(Collectors.toList());
        atPersonList = vo.getAtPersons().stream().map(SNSDingTalkAtPersonInventory::valueOf).collect(Collectors.toList());
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

    public List<String> getAtPersonPhoneNumbers() {
        return atPersonPhoneNumbers;
    }

    public void setAtPersonPhoneNumbers(List<String> atPersonPhoneNumbers) {
        this.atPersonPhoneNumbers = atPersonPhoneNumbers;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<SNSDingTalkAtPersonInventory> getAtPersonList() {
        return atPersonList;
    }

    public void setAtPersonList(List<SNSDingTalkAtPersonInventory> atPersonList) {
        this.atPersonList = atPersonList;
    }

    public SNSDingTalkEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSDingTalkEndpointInventory valueOf(SNSDingTalkEndpointVO vo) {
        return new SNSDingTalkEndpointInventory(vo);
    }

    public static List<SNSDingTalkEndpointInventory> valueOf1(Collection<SNSDingTalkEndpointVO> vos) {
        return vos.stream().map(SNSDingTalkEndpointInventory::valueOf).collect(Collectors.toList());
    }
}
