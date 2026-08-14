package org.zstack.header.vm;

import org.zstack.header.image.*;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/9/21.
 */
@RestResponse(allTo = "inventories")
public class APIGetCandidateIsoForAttachingVmReply extends APIReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetCandidateIsoForAttachingVmReply __example__() {
        APIGetCandidateIsoForAttachingVmReply reply = new APIGetCandidateIsoForAttachingVmReply();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("ceph://zs-images/f0b149e053b34c7eb7fe694b182ebffd");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("TinyLinux");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2");
        inv.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());

        reply.setInventories(asList(inv));

        return reply;
    }

}
