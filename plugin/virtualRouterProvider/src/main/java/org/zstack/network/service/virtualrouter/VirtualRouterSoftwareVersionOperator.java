package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSoftwareVersionOperator {
    @Autowired
    DatabaseFacade dbf;

    public void updateVirtualRouterSoftwareVersion(VirtualRouterSoftwareVersionStruct struct) {
        VirtualRouterSoftwareVersionVO vo = Q.New(VirtualRouterSoftwareVersionVO.class)
                .eq(VirtualRouterSoftwareVersionVO_.uuid, struct.getVrUuid())
                .eq(VirtualRouterSoftwareVersionVO_.softwareName, "IPsec")
                .find();
        boolean update = false;
        if (vo != null) {
            if (struct.getCurrentVersion() != null && !struct.getCurrentVersion().equals(vo.getCurrentVersion())) {
                vo.setCurrentVersion(struct.getCurrentVersion());
                update = true;
            }

            if (struct.getLatestVersion() != null && !struct.getLatestVersion().equals(vo.getLatestVersion())) {
                vo.setLatestVersion(struct.getLatestVersion());
                update = true;
            }

            if (update) {
                dbf.update(vo);
            }
        } else {
            vo = new VirtualRouterSoftwareVersionVO();
            vo.setUuid(struct.getVrUuid());
            vo.setSoftwareName(struct.getSoftwareName());

            if (struct.getCurrentVersion() != null) {
                vo.setCurrentVersion(struct.getCurrentVersion());
                update = true;
            }

            if (struct.getLatestVersion() != null) {
                vo.setLatestVersion(struct.getLatestVersion());
                update = true;
            }

            if (update) {
                dbf.persist(vo);
            }
        }
    }
}
