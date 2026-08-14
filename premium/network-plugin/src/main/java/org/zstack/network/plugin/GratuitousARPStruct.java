package org.zstack.network.plugin;

import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

/**
 * Created by shixin.ruan on 10/13/2015.
 */
public class GratuitousARPStruct {
    private VmInstanceInventory vm;

    public VmInstanceInventory getVm() {
        return vm;
    }

    public void setVm(VmInstanceInventory vm) {
        this.vm = vm;
    }
}
