package org.zstack.kvm.vmfiles;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.SQL;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;

import java.util.Objects;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractVmHostBackupFileBase {
    protected VmHostBackupFileVO self;

    protected AbstractVmHostBackupFileBase(VmHostBackupFileVO self) {
        this.self = Objects.requireNonNull(self);
    }

    public abstract VmHostFileType type();

    public void afterBackup(VmHostBackupFileVO from) {
    }

    public void afterBackup(VmHostFileVO from) {
    }

    public void clean() {
        SQL.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.uuid, self.getUuid())
                .delete();
    }
}
