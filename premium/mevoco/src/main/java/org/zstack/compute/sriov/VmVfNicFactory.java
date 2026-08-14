package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.sriov.EthernetVfPciDeviceVO;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicManager;
import org.zstack.header.sriov.VmVfNicVO;
import org.zstack.header.vm.*;
import org.zstack.identity.Account;
import org.zstack.pciDevice.PciDeviceInventory;
import org.zstack.tag.TagManager;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.zstack.core.Platform.err;

/**
 * Created by GuoYi on 11/28/19.
 */
public class VmVfNicFactory implements VmInstanceNicFactory {
    private static final CLogger logger = Utils.getLogger(VmVfNicFactory.class);
    private static final VmNicType type = new VmNicType(VmVfNicConstant.VIRTUAL_FUNCTION_TYPE, true);
    private static final VSwitchType vSwitchType = new VSwitchType(L2NetworkConstant.VSWITCH_TYPE_LINUX_BRIDGE, type);
    private static final VfPciDeviceUtils vfPciDeviceUtils = new VfPciDeviceUtils();
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmVfNicManager vmVfMgr;
    @Autowired
    private CloudBus bus;
    @Autowired
    private TagManager tagMgr;
    @Override
    public VmNicType getType() {
        return type;
    }

    @Override
    public VSwitchType getVSwitchType() {
        return vSwitchType;
    }

    @Override
    public VmNicVO createVmNic(VmNicInventory nic, VmInstanceSpec spec) {
        String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

        PciDeviceInventory pci = vfPciDeviceUtils.allocateReservedVfDevice(nic);
        VmNicVO vnic = VmInstanceNicFactory.createVmNic(nic);
        VmVfNicVO vf = new VmVfNicVO(vnic);
        vf.setType(type.toString());
        vf.setAccountUuid(acntUuid);
        vf.setPciDeviceUuid(pci.getUuid());

        vf = persistAndRetryIfMacCollision(vf);

        if (vf == null) {
            throw new FlowException(err(VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }

        vf = dbf.reload(vf);
        spec.getDestNics().add(VmNicInventory.valueOf(vf));
        return vf;
    }

    private VmVfNicVO persistAndRetryIfMacCollision(VmVfNicVO vo) {
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
    public boolean addFdbForEipNameSpace(VmNicInventory nic) {
        return nic.getType().equals(type.toString());
    }

    @Override
    public String getPhysicalNicName(VmNicInventory nic) {
        if (!nic.getType().equals(type.toString())) {
            return null;
        }

        VmVfNicVO vfNic = dbf.findByUuid(nic.getUuid(), VmVfNicVO.class);
        EthernetVfPciDeviceVO vfPciDeviceVO = dbf.findByUuid(vfNic.getPciDeviceUuid(), EthernetVfPciDeviceVO.class);
        return vfPciDeviceVO.getInterfaceName();
    }

    @Override
    public void releaseVmNic(VmNicInventory nic) {
        vfPciDeviceUtils.releaseVfDevice(nic);
    }
}
