package org.zstack.billing.generator.volume.root;

import org.zstack.billing.BillingConstants;
import org.zstack.billing.generator.BillingInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/5/28.
 */
@Inventory(mappingVOClass = RootVolumeBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_ROOT_VOLUME)})
public class RootVolumeBillingInventory extends BillingInventory {
    private String vmInstanceUuid;

    private long volumeSize;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public RootVolumeBillingInventory() {
    }

    public RootVolumeBillingInventory(RootVolumeBillingVO vo) {
        super(vo);
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
        this.setVolumeSize(vo.getVolumeSize());
    }

    public static RootVolumeBillingInventory valueOf(RootVolumeBillingVO vo) {
        return new RootVolumeBillingInventory(vo);
    }

    public static List<RootVolumeBillingInventory> valueOf1(Collection<RootVolumeBillingVO> vos) {
        List<RootVolumeBillingInventory> invs = new ArrayList<RootVolumeBillingInventory>();
        for (RootVolumeBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
