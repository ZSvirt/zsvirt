package org.zstack.header.vm;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;

import java.util.List;

public class CloneVmInstanceReply extends MessageReply {
    private CloneVmInstanceResults results;
    private List<ImageInventory> tempImages;

    public List<ImageInventory> getTempImages() {
        return tempImages;
    }

    public void setTempImages(List<ImageInventory> tempImages) {
        this.tempImages = tempImages;
    }

    public CloneVmInstanceResults getResults() {
        return results;
    }

    public void setResults(CloneVmInstanceResults results) {
        this.results = results;
    }
}
