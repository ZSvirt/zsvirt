package org.zstack.billing.generator.volume.data;

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
@Inventory(mappingVOClass = DataVolumeBillingVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BillingInventory.class, type = BillingConstants.SPENDING_TYPE_DATA_VOLUME)})
public class DataVolumeBillingInventory extends BillingInventory {
    private long volumeSize;

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public DataVolumeBillingInventory() {
    }

    public DataVolumeBillingInventory(DataVolumeBillingVO vo) {
        super(vo);
        this.setVolumeSize(vo.getVolumeSize());
    }

    public static DataVolumeBillingInventory valueOf(DataVolumeBillingVO vo) {
        return new DataVolumeBillingInventory(vo);
    }

    public static List<DataVolumeBillingInventory> valueOf1(Collection<DataVolumeBillingVO> vos) {
        List<DataVolumeBillingInventory> invs = new ArrayList<DataVolumeBillingInventory>();
        for (DataVolumeBillingVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
