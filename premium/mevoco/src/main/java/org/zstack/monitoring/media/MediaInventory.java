package org.zstack.monitoring.media;

import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/6/11.
 */
@Inventory(mappingVOClass = MediaVO.class)
public class MediaInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    @TypeField
    private String type;
    private String state;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public MediaInventory() {
    }

    public MediaInventory(MediaVO vo) {
        uuid = vo.getUuid();
        name = vo.getName();
        description = vo.getDescription();
        type = vo.getType();
        state = vo.getState().toString();
        lastOpDate = vo.getLastOpDate();
        createDate = vo.getCreateDate();
    }

    public static MediaInventory valueOf(MediaVO vo) {
        return new MediaInventory(vo);
    }

    public static List<MediaInventory> valueOf(Collection<MediaVO> vos) {
        List<MediaInventory> invs = new ArrayList<>();
        for (MediaVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
