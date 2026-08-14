package org.zstack.core.jsonlabel;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/9/13.
 */
@Inventory(mappingVOClass = JsonLabelVO.class)
@PythonClassInventory
public class JsonLabelInventory {
    private long id;
    private String labelKey;
    private String labelValue;
    private String resourceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static JsonLabelInventory valueOf(JsonLabelVO vo) {
        JsonLabelInventory inv = new JsonLabelInventory();
        inv.setId(vo.getId());
        inv.setLabelKey(vo.getLabelKey());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setLabelValue(vo.getLabelValue());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<JsonLabelInventory> valueOf(Collection<JsonLabelVO> vos) {
        List<JsonLabelInventory> invs = new ArrayList<>();
        for (JsonLabelVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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
