package org.zstack.directory;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * @author shenjin
 * @date 2022/12/11 11:17
 */
public class VmDirectoryChecker implements DirectoryChecker{
    public static DirectoryType type = new DirectoryType(VmInstanceVO.class.getSimpleName());

    @Override
    public ErrorCode check(String directoryUuid, List<String> resourceUuids) {
        DirectoryVO vo = Q.New(DirectoryVO.class).eq(DirectoryVO_.uuid, directoryUuid).find();
        List<String> zoneUuids = Q.New(VmInstanceVO.class).select(VmInstanceVO_.zoneUuid)
                .in(VmInstanceVO_.uuid, resourceUuids).listValues();
        if(zoneUuids.stream().allMatch(s -> s.equals(vo.getZoneUuid()))) {
            return null;
        } else {
            return argerr("all resources zoneUuid must be consistent with the directory zoneUuid[%s]", vo.getZoneUuid());
        }
    }

    @Override
    public DirectoryType getType() {
        return type;
    }
}
