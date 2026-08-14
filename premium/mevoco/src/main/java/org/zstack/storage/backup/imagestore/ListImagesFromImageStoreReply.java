package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/8/31.
 */
public class ListImagesFromImageStoreReply extends MessageReply {
    private List<ImageStoreImageStruct> structs = new ArrayList<>();

    public List<ImageStoreImageStruct> getStructs() {
        return structs;
    }

    public void setStructs(List<ImageStoreImageStruct> structs) {
        this.structs = structs;
    }
}
