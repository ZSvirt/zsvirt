package org.zstack.header.core.webhooks;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/5/7.
 */
@Inventory(mappingVOClass = WebhookVO.class)
public class WebhookInventory {
    private String uuid;
    private String name;
    private String description;
    private String url;
    private String type;
    private String opaque;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static WebhookInventory valueOf(WebhookVO vo) {
        WebhookInventory inv = new WebhookInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setUrl(vo.getUrl());
        inv.setType(vo.getType());
        inv.setOpaque(vo.getOpaque());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<WebhookInventory> valueOf(Collection<WebhookVO> vos) {
        List<WebhookInventory> invs = new ArrayList<>();
        for (WebhookVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOpaque() {
        return opaque;
    }

    public void setOpaque(String opaque) {
        this.opaque = opaque;
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
