package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionDSL;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by mingjian.deng on 2017/8/31.
 */
@RestResponse(allTo = "infos")
public class APIGetImagesFromImageStoreBackupStorageReply extends APIReply {
    List<ImageStoreImageStruct> infos = new ArrayList<>();

    public List<ImageStoreImageStruct> getInfos() {
        return infos;
    }

    public void setInfos(List<ImageStoreImageStruct> infos) {
        this.infos = infos;
        this.infos.sort(new Comparator<ImageStoreImageStruct>() {
            @Override
            public int compare(ImageStoreImageStruct o1, ImageStoreImageStruct o2) {
                if (o1.getName().compareTo(o2.getName()) < 0) {
                    return -1;
                } else if (o1.getName().compareTo(o2.getName()) > 0) {
                    return 1;
                } else {
                    if (o1.getCreated().getTime() < o2.getCreated().getTime()) {
                        return -1;
                    } else if (o1.getCreated().getTime() > o2.getCreated().getTime()){
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        });
    }

    public static APIGetImagesFromImageStoreBackupStorageReply __example__() {
        APIGetImagesFromImageStoreBackupStorageReply reply = new APIGetImagesFromImageStoreBackupStorageReply();
        ImageStoreImageStruct struct = new ImageStoreImageStruct();
        struct.setName(uuid());
        struct.setId("270c67e3699f72ba");
        struct.setAuthor("zstack");
        struct.setArch("amd64");
        struct.setSize(7995392L);
        struct.setVirtualsize(12682240L);
        struct.setCreated(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInfos(CollectionDSL.list(struct));

        return reply;
    }
}
