package org.zstack.tag2;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tag.TagPatternInventory;
import org.zstack.header.tag.TagPatternType;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIQueryTagReply extends APIQueryReply {
    private List<TagPatternInventory> inventories;

    public List<TagPatternInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<TagPatternInventory> inventories) {
        this.inventories = inventories;
    }


    public static APIQueryTagReply __example__() {
        APIQueryTagReply reply = new APIQueryTagReply();
        TagPatternInventory tag = new TagPatternInventory();
        tag.setType(TagPatternType.simple);
        tag.setValue("SSD");
        tag.setName("SSD");
        tag.setColor("#FFFFFF");
        tag.setDescription("SSD volume");
        tag.setUuid(uuid());
        tag.setCreateDate(new Timestamp(DocUtils.date));
        tag.setLastOpDate(new Timestamp(DocUtils.date));
        reply.setInventories(Collections.singletonList(tag));
        return reply;
    }
}

