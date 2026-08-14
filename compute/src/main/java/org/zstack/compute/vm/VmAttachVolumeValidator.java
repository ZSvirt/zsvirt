package org.zstack.compute.vm;

import org.zstack.core.ScatteredValidator;
import org.zstack.header.vm.VmAttachVolumeValidatorMethod;
import org.zstack.header.vm.VmInstanceInventory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by MaJin on 2017-05-31.
 */
public class VmAttachVolumeValidator extends ScatteredValidator {
    private static List<Method> methods;
    
    static {
        // method signature: static void xxx(String vmUuid, String volumeUuid)
        methods = collectValidatorMethods(VmAttachVolumeValidatorMethod.class, VmInstanceInventory.class, String.class);
    }

    public void validate(VmInstanceInventory vmInv, String volumeUuid) {
        invokeValidatorMethods(methods, vmInv, volumeUuid);
    }
}
