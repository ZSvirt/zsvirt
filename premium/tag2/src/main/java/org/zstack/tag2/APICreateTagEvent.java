package org.zstack.tag2;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.tag.TagPatternInventory;

@RestResponse(allTo = "inventory")
public class APICreateTagEvent extends APIEvent {
    private TagPatternInventory inventory;

    public APICreateTagEvent(String apiId) {
        super(apiId);
    }

    public APICreateTagEvent() {
        super();
    }

    public TagPatternInventory getInventory() {
        return inventory;
    }

    public void setInventory(TagPatternInventory inventory) {
        this.inventory = inventory;
    }
}
