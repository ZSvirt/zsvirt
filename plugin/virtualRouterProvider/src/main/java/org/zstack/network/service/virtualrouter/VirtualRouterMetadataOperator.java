package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.VersionComparator;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterMetadataOperator {
    @Autowired
    DatabaseFacade dbf;

    public void updateVirtualRouterMetadata(VirtualRouterMetadataStruct struct) {
        VirtualRouterMetadataVO vo = dbf.findByUuid(struct.getVrUuid(), VirtualRouterMetadataVO.class);
        boolean update = false;
        if (vo != null) {
            if (zvrVersionCheck(struct.getZvrVersion()) && !struct.getZvrVersion().equals(vo.getZvrVersion())) {
                vo.setZvrVersion(struct.getZvrVersion());
                update = true;
            }

            if (struct.getVyosVersion() != null && !struct.getVyosVersion().equals(vo.getVyosVersion())) {
                vo.setVyosVersion(struct.getVyosVersion());
                update = true;
            }

            if (struct.getKernelVersion() != null && !struct.getKernelVersion().equals(vo.getKernelVersion())) {
                vo.setKernelVersion(struct.getKernelVersion());
                update = true;
            }

            if (update) {
                dbf.update(vo);
            }
        } else {
            vo = new VirtualRouterMetadataVO();
            vo.setUuid(struct.getVrUuid());

            if (zvrVersionCheck(struct.getZvrVersion())) {
                vo.setZvrVersion(struct.getZvrVersion());
                update = true;
            }

            if (struct.getVyosVersion() != null) {
                vo.setVyosVersion(struct.getVyosVersion());
                update = true;
            }

            if (struct.getKernelVersion() != null) {
                vo.setKernelVersion(struct.getKernelVersion());
                update = true;
            }

            if (update) {
                dbf.persist(vo);
            }
        }
    }

    public String getZvrVersion(String vrUuid) {
        VirtualRouterMetadataVO vo = dbf.findByUuid(vrUuid, VirtualRouterMetadataVO.class);
        if (vo == null) {
            return null;
        }

        return vo.getZvrVersion();
    }

    public static boolean zvrVersionCheck(String zvrVersion) {
        if (zvrVersion == null) {
            return false;
        }

        String[] items = zvrVersion.split("\\.");
        if (items.length != VyosConstants.VYOS_VERSION_LENGTH ){
            return false;
        }

        try {
            new VersionComparator(zvrVersion);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
