package org.zstack.storage.device.iscsi;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.device.StorageDeviceState;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

/**
 * Create by weiwang at 2018/8/2
 */

@RestResponse(allTo = "inventory")
public class APIDetachIscsiServerFromClusterEvent extends APIEvent {
    private IscsiServerInventory inventory;

    public IscsiServerInventory getInventory() {
        return inventory;
    }

    public void setInventory(IscsiServerInventory inventory) {
        this.inventory = inventory;
    }

    public APIDetachIscsiServerFromClusterEvent(String apiId) {
        super(apiId);
    }

    public APIDetachIscsiServerFromClusterEvent() {
        super(null);
    }

    public static APIDetachIscsiServerFromClusterEvent __example__() {
        APIDetachIscsiServerFromClusterEvent event = new APIDetachIscsiServerFromClusterEvent();

        IscsiLunInventory iscsiLunInventory = new IscsiLunInventory();
        iscsiLunInventory.setUuid(uuid());
        iscsiLunInventory.setWwn("0x6b083fe000daf018");
        iscsiLunInventory.setHctl("6:0:1:1");
        iscsiLunInventory.setModel("MD32xx");
        iscsiLunInventory.setVendor("DELL");
        iscsiLunInventory.setType("mpath");
        iscsiLunInventory.setSerial("6b083fe000daf018000015505abbe00a");
        iscsiLunInventory.setSize(30003188203520l);
        iscsiLunInventory.setWwid("36b083fe000daf018000015505abbe00a");
        iscsiLunInventory.setPath("ip-0.0.0.201:3260-iscsi-iqn.2018-01.io.zstack:tsn.00001-lun-0");
        iscsiLunInventory.setMultipathDeviceUuid("36b083fe000daf018000015505abbe00a");

        IscsiLunInventory iscsiLunInventory2 = new IscsiLunInventory();
        iscsiLunInventory2.setUuid(uuid());
        iscsiLunInventory2.setWwn("0x6b083fe000daf018");
        iscsiLunInventory2.setHctl("6:0:3:1");
        iscsiLunInventory2.setModel("MD32xx");
        iscsiLunInventory2.setVendor("DELL");
        iscsiLunInventory2.setType("mpath");
        iscsiLunInventory2.setSerial("6b083fe000daf018000015505abbe00a");
        iscsiLunInventory2.setSize(30003188203520l);
        iscsiLunInventory2.setWwid("36b083fe000daf018000015505abbe00a");
        iscsiLunInventory2.setPath("ip-0.0.0.201:3260-iscsi-iqn.2018-01.io.zstack:tsn.00002-lun-0");
        iscsiLunInventory2.setMultipathDeviceUuid("36b083fe000daf018000015505abbe00a");

        IscsiTargetInventory iscsiTargetInventory = new IscsiTargetInventory();
        iscsiTargetInventory.setUuid(uuid());
        iscsiTargetInventory.setState(StorageDeviceState.Enabled.toString());
        iscsiTargetInventory.setIqn("iqn.2018-01.io.zstack:tsn.00001");
        iscsiTargetInventory.setCreateDate(new Timestamp(DocUtils.date));
        iscsiTargetInventory.setLastOpDate(new Timestamp(DocUtils.date));
        iscsiTargetInventory.setIscsiLuns(asList(iscsiLunInventory));
        iscsiLunInventory.setIscsiTargetUuid(iscsiTargetInventory.getUuid());

        IscsiTargetInventory iscsiTargetInventory2 = new IscsiTargetInventory();
        iscsiTargetInventory2.setUuid(uuid());
        iscsiTargetInventory2.setState(StorageDeviceState.Enabled.toString());
        iscsiTargetInventory2.setIqn("iqn.2018-01.io.zstack:tsn.00002");
        iscsiTargetInventory2.setCreateDate(new Timestamp(DocUtils.date));
        iscsiTargetInventory2.setLastOpDate(new Timestamp(DocUtils.date));
        iscsiTargetInventory2.setIscsiLuns(asList(iscsiLunInventory2));
        iscsiLunInventory2.setIscsiTargetUuid(iscsiTargetInventory2.getUuid());

        IscsiServerInventory iscsiServerInventory = new IscsiServerInventory();
        iscsiServerInventory.setUuid(uuid());
        iscsiServerInventory.setState(StorageDeviceState.Enabled.toString());
        iscsiServerInventory.setIp("10.0.0.201");
        iscsiServerInventory.setPort(3260);
        iscsiServerInventory.setChapUserName("username");
        iscsiServerInventory.setChapUserPassword("password");
        iscsiServerInventory.setCreateDate(new Timestamp(DocUtils.date));
        iscsiServerInventory.setLastOpDate(new Timestamp(DocUtils.date));
        iscsiServerInventory.setIscsiTargets(asList(iscsiTargetInventory));

        iscsiTargetInventory.setIscsiServerUuid(iscsiServerInventory.getUuid());

        ClusterInventory cluster = new ClusterInventory();
        cluster.setHypervisorType("KVM");
        cluster.setName("cluster1");
        cluster.setDescription("test");
        cluster.setState(ClusterState.Enabled.toString());
        cluster.setZoneUuid(uuid());
        cluster.setUuid(uuid());
        cluster.setType("zstack");
        cluster.setCreateDate(new Timestamp(DocUtils.date));
        cluster.setLastOpDate(new Timestamp(DocUtils.date));

        IscsiServerClusterRefInventory refInventory = new IscsiServerClusterRefInventory();
        refInventory.setId(1);
        refInventory.setIscsiServerUuid(iscsiServerInventory.getUuid());
        refInventory.setClusterUuid(cluster.getUuid());
        refInventory.setCreateDate(new Timestamp(DocUtils.date));
        refInventory.setLastOpDate(new Timestamp(DocUtils.date));

        iscsiServerInventory.setIscsiClusterRefs(asList(refInventory));

        event.setInventory(iscsiServerInventory);
        event.setSuccess(true);
        return event;
    }
}
