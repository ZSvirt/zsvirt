package org.zstack.header.tag;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = TagPatternVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "tag", inventoryClass = UserTagInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "tagPatternUuid")
})
public class TagPatternInventory {
    private String uuid;

    private String name;

    private String value;

    private String description;

    private String color;

    private TagPatternType type;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public static TagPatternInventory valueOf(TagPatternVO vo) {
        TagPatternInventory inv = new TagPatternInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.description = vo.getDescription();
        inv.value = vo.getValue();
        inv.color = vo.getColor();
        inv.type = vo.getType();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<TagPatternInventory> valueOf(Collection<TagPatternVO> vos) {
        return vos.stream().map(TagPatternInventory::valueOf).collect(Collectors.toList());
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagPatternType getType() {
        return type;
    }

    public void setType(TagPatternType type) {
        this.type = type;
    }
}
