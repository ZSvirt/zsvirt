package org.zstack.kvm.vmfiles;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;

import java.util.Objects;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractVmHostFileBase {
    protected VmHostFileVO self;

    protected AbstractVmHostFileBase(VmHostFileVO self) {
        this.self = Objects.requireNonNull(self);
    }

    public abstract VmHostFileType type();
}
