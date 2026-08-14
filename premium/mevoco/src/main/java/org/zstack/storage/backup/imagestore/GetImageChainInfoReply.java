package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetImageChainInfoReply extends MessageReply {
    private List<ImageStoreImageResponse> chain;

    public List<ImageStoreImageResponse> getChain() {
        return chain;
    }

    public void setChain(List<ImageStoreImageResponse> chain) {
        this.chain = chain;
    }
}
