package org.zstack.sns;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSTopicVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "endpointRef", inventoryClass = SNSSubscriberInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "topicUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "endpoints", expandedField = "endpointRef.endpoints")
})
public class SNSTopicInventory {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private String locale;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SNSTopicInventory __example__() {
        SNSTopicInventory inv = new SNSTopicInventory();
        inv.setUuid(DocUtils.createFixedUuid(SNSTopicVO.class));
        inv.setName("new name");
        inv.setState(SNSTopicState.Enabled.toString());
        inv.setLocale("zh_CN");
        inv.setCreateDate(DocUtils.timestamp());
        inv.setLastOpDate(DocUtils.timestamp());
        return inv;
    }

    public static SNSTopicInventory valueOf(SNSTopicVO vo) {
        SNSTopicInventory inv = new SNSTopicInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.state = vo.getState().toString();
        inv.description = vo.getDescription();
        inv.locale = vo.getLocale();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<SNSTopicInventory> valueOf(Collection<SNSTopicVO> vos) {
        return vos.stream().map(SNSTopicInventory::valueOf).collect(Collectors.toList());
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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
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
