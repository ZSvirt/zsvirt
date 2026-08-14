package org.zstack.sns.platform.http;

import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationEndpointVO;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSSubscriberInventory;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSHttpEndpointVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "platform", inventoryClass = SNSApplicationPlatformInventory.class,
                foreignKey = "platformUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "topicRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "endpointUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "topics", expandedField = "topicRef.topics")
})
public class SNSHttpEndpointInventory extends SNSApplicationEndpointInventory implements Serializable {
    private String url;
    private String username;
    @APINoSee
    @NoLogging
    private String password;

    public static SNSHttpEndpointInventory __example__() {
        SNSHttpEndpointInventory inv = new SNSHttpEndpointInventory(SNSApplicationEndpointInventory.__example__());
        inv.setUrl("http://127.0.0.1:6666/test-url");
        inv.setUsername("username");
        inv.setPassword("password");
        return inv;
    }

    public SNSHttpEndpointInventory() {
    }

    public SNSHttpEndpointInventory(SNSApplicationEndpointVO vo) {
        super(vo);
    }

    public SNSHttpEndpointInventory(SNSApplicationEndpointInventory other) {
        super(other);
    }

    public static SNSHttpEndpointInventory valueOf(SNSHttpEndpointVO vo) {
        SNSHttpEndpointInventory inv = new SNSHttpEndpointInventory(vo);
        inv.url = vo.getUrl();
        inv.username = vo.getUsername();
        inv.password = vo.getPassword();
        return inv;
    }

    public static List<SNSHttpEndpointInventory> valueOf1(Collection<SNSHttpEndpointVO> vos) {
        return vos.stream().map(SNSHttpEndpointInventory::valueOf).collect(Collectors.toList());
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
