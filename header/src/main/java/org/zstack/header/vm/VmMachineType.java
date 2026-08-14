package org.zstack.header.vm;

import org.zstack.header.exception.CloudRuntimeException;

/**
 * @author: kefeng.wang
 * @date: 2018-09-25 16:12
 **/
public enum VmMachineType {
    pc,
    q35,
    virt;

    public static VmMachineType get(String value) {
        try {
            return VmMachineType.valueOf(value);
        } catch (Exception e){
            return null;
        }
    }
}
