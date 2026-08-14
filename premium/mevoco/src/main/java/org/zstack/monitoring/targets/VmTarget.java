package org.zstack.monitoring.targets;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.items.vm.VmItem;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/6/26.
 */
public class VmTarget extends MonitorTarget {
    private VmInstanceInventory inventory;

    private static List<Item> vmItems;

    static {
        vmItems = Platform.getReflections().getSubTypesOf(VmItem.class).stream()
                .filter( clz -> !Modifier.isAbstract(clz.getModifiers()))
                .map( clz -> {
                    try {
                        return clz.newInstance();
                    } catch (Exception e) {
                        throw new CloudRuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    public VmTarget(VmInstanceInventory inventory) {
        super(vmItems);
        this.inventory = inventory;
    }

    public VmInstanceInventory getInventory() {
        return inventory;
    }

    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getTargetResourceUuid() {
        return inventory.getUuid();
    }
}
