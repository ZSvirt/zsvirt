package org.zstack.header.vm;

import org.zstack.header.image.*;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.search.APIGetReply;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by GuoYi on 11/2/17.
 */
@RestResponse(allTo = "inventories")
public class APIGetImageCandidatesForVmToChangeReply extends APIGetReply {
    private List<ImageInventory> inventories;

    public List<ImageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetImageCandidatesForVmToChangeReply __example__() {
        APIGetImageCandidatesForVmToChangeReply reply = new APIGetImageCandidatesForVmToChangeReply();

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
