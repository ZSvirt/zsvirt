package org.zstack.header.vm;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.header.network.l3.L3NetworkInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class VmNicSpec implements Serializable {
    /* due to the design changed, single nic will only has 1 l3 network */
    private List<L3NetworkInventory> l3Invs;
    private String nicDriverType;

    //from api msg
    /* due to the design changed, single nic will only has 1 l3 network */
    private List<VmNicParam> vmNicParams;

    public VmNicSpec(List<L3NetworkInventory> l3Invs) {
        this.l3Invs = l3Invs;
    }

    public VmNicSpec(List<L3NetworkInventory> l3Invs, String nicDriverType) {
        this.l3Invs = l3Invs;
        this.nicDriverType = nicDriverType;
    }

    public VmNicSpec(L3NetworkInventory l3) {
        this.l3Invs = list(l3);
    }

    public VmNicSpec(L3NetworkInventory l3, String nicDriverType) {
        this.l3Invs = list(l3);
        this.nicDriverType = nicDriverType;
    }

    public List<L3NetworkInventory> getL3Invs() {
        return l3Invs;
    }

    public void setL3Invs(List<L3NetworkInventory> l3Invs) {
        this.l3Invs = l3Invs;
    }

    public String getNicDriverType() {
        return nicDriverType;
    }

    public void setNicDriverType(String nicDriverType) {
        this.nicDriverType = nicDriverType;
    }

    public static List<L3NetworkInventory> getL3NetworkInventoryOfSpec(List<VmNicSpec> specs) {
        List<L3NetworkInventory> res = new ArrayList<>();

        if (CollectionUtils.isEmpty(specs)) {
            return res;
        }

        for (VmNicSpec spec: specs) {
            res.addAll(spec.l3Invs);
        }
        return res;
    }

    public static List<VmNicSpec> getFirstL3NetworkInventoryOfSpec(List<VmNicSpec> specs) {
        List<VmNicSpec> nicSpecs =  new ArrayList<>();
        for (VmNicSpec spec: specs) {
            if (spec.l3Invs != null && !spec.l3Invs.isEmpty()) {
                nicSpecs.add(spec);
            }
        }
        return nicSpecs;
    }

    public static List<String> getL3UuidsOfSpec(List<VmNicSpec> specs) {
        List<String> res = new ArrayList<>();
        for (VmNicSpec spec: specs) {
            for (L3NetworkInventory inv : spec.l3Invs) {
                res.add(inv.getUuid());
            }
        }
        return res;
    }

    public static List<VmNicParam> getVmNicParamsOfSpec(List<VmNicSpec> specs) {
        List<VmNicParam> vmNicParams = new ArrayList<>();
        for (VmNicSpec spec: specs) {
            if (spec.vmNicParams != null) {
                vmNicParams.addAll(spec.vmNicParams);
            }
        }
        return vmNicParams;
    }

    public List<VmNicParam> getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(List<VmNicParam> vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public VmNicParam getVmNicParam() {
        if (CollectionUtils.isEmpty(this.vmNicParams)) {
            return new VmNicParam();
        }

        return this.vmNicParams.get(0);
    }
}
