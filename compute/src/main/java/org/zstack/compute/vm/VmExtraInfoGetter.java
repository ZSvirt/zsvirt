package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.volume.VolumeType;

import javax.persistence.Tuple;
import java.util.List;

/**
 * Created by MaJin on 2021/1/8.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExtraInfoGetter {
    @Autowired
    private DatabaseFacade dbf;

    private String uuid;

    private VmExtraInfoGetter(String uuid) {
        this.uuid = uuid;
    }

    public static VmExtraInfoGetter New(String uuid) {
        return new VmExtraInfoGetter(uuid);
    }

    public boolean isVirtio() {
        return VmSystemTags.VIRTIO.hasTag(uuid);
    }
}
