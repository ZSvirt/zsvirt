package org.zstack.compute.vdpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.sriov.VfPciDeviceUtils;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.network.l2.*;
import org.zstack.header.vdpa.VmVdpaNicConstant;
import org.zstack.header.vdpa.VmVdpaNicVO;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.zstack.core.Platform.err;


/**
 * Created by haibiao.xiao on 3/23/2021.
 */
public class VmVdpaNicFactory implements VmInstanceNicFactory {
    private static final CLogger logger = Utils.getLogger(VmVdpaNicFactory.class);
    private static final VmNicType type = new VmNicType(VmVdpaNicConstant.VIRTIO_DATA_PATH_ACCEL_TYPE, true);
    private static final VSwitchType vSwitchType = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_OVS_DPDK, type);

    @Autowired
    private DatabaseFacade dbf;

    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();

    @Override
    public VmNicType getType() {
        return type;
    }

    @Override
    public VSwitchType getVSwitchType() {
        return vSwitchType;
    }

    public static String generateSrcPath(VmVdpaNicVO vdpaVO) {
        return String.format("/var/run/zstack/vdpa/%s/%s", vdpaVO.getVmInstanceUuid(), vdpaVO.getInternalName());
    }

    @Override
    public VmNicVO createVmNic(VmNicInventory nic, VmInstanceSpec spec) {
        String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

        PciDeviceInventory pci = vfPciDeviceUtils.allocateReservedVfDevice(nic);

        VmNicVO vnic = VmInstanceNicFactory.createVmNic(nic);
        VmVdpaNicVO vdpa = new VmVdpaNicVO(vnic);
        vdpa.setType(type.toString());
        vdpa.setAccountUuid(acntUuid);
        vdpa.setPciDeviceUuid(pci.getUuid());
        vdpa.setSrcPath(generateSrcPath(vdpa));

        vdpa = persistAndRetryIfMacCollision(vdpa);

        if (vdpa == null) {
            throw new FlowException(err(VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }
        vdpa = dbf.reload(vdpa);
        spec.getDestNics().add(VmNicInventory.valueOf(vdpa));
        return vdpa;
    }

    private VmVdpaNicVO persistAndRetryIfMacCollision(VmVdpaNicVO vo) {
        int tries = 5;
        while (tries-- > 0) {
            try {
                return dbf.persistAndRefresh(vo);
            } catch (PersistenceException e) {
                if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                    logger.debug(String.format("Concurrent mac allocation. Mac[%s] has been allocated, try allocating another one. " +
                            "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                            "we will try finding another mac", vo.getMac()));
                    logger.trace("", e);
                    vo.setMac(NetworkUtils.generateMacWithDeviceId((short) vo.getDeviceId()));
                } else {
                    throw e;
                }
            }
        }
        return null;
    }

    @Override
    public void releaseVmNic(VmNicInventory nic) {
        vfPciDeviceUtils.releaseVfDevice(nic);
    }
}