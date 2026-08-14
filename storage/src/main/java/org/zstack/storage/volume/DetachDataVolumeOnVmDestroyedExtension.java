package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmReleaseResourceExtensionPoint;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.Query;

public class DetachDataVolumeOnVmDestroyedExtension implements VmReleaseResourceExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    
    @Transactional
    private void detachDataVolume(VmInstanceInventory inv) {
        dbf.entityForTranscationCallback(Operation.UPDATE, VolumeVO.class);
        
        String sql = "update VolumeVO vol set vol.vmInstanceUuid = NULL, vol.deviceId = NULL where vol.vmInstanceUuid = :vmUuid and vol.type = :volType";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("vmUuid", inv.getUuid());
        q.setParameter("volType", VolumeType.Data);
        q.executeUpdate();
    }
    
    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        VmInstanceInventory inv = spec.getVmInventory();
        if (spec.getCurrentVmOperation() != VmOperation.Destroy) {
            completion.success();
            return;
        }
        
        // only has root volume
        if (inv.getAllVolumes().size() == 1) {
            completion.success();
            return;
        }
        
        detachDataVolume(inv);
        completion.success();
    }
}
