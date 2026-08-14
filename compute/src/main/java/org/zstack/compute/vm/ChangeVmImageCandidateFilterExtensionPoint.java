package org.zstack.compute.vm;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.List;

/**
 * Created by GuoYi on 3/10/21.
 */
public interface ChangeVmImageCandidateFilterExtensionPoint {
    void filterImageCandidates(VmInstanceInventory vm, List<ImageInventory> candidates);
}
