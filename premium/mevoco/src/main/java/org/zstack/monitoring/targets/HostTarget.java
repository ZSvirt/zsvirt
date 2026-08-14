package org.zstack.monitoring.targets;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.monitoring.items.Item;
import org.zstack.monitoring.items.host.HostItem;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/6/2.
 */
public class HostTarget extends MonitorTarget {
    private HostInventory inventory;

    private static List<Item> hostItems;

    static {
        hostItems = Platform.getReflections().getSubTypesOf(HostItem.class).stream()
                .filter( clz -> !Modifier.isAbstract(clz.getModifiers()))
                .map( clz -> {
                    try {
                        return clz.newInstance();
                    } catch (Exception e) {
                        throw new CloudRuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }


    public HostTarget(HostInventory inventory) {
        super(hostItems);
        this.inventory = inventory;
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public String getTargetResourceUuid() {
        return inventory.getUuid();
    }
}
