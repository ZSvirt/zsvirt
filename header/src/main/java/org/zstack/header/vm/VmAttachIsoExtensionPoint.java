package org.zstack.header.vm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/11/20.
 */
public interface VmAttachIsoExtensionPoint {
    ErrorCode filtCandidateVms(String isoUuid, List<VmInstanceInventory> vms);
    void filtCandidateIsos(String vmUuid, List<ImageInventory> isos);
}
