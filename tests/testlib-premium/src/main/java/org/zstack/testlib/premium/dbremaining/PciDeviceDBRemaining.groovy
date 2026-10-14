package org.zstack.testlib.premium.dbremaining

import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO
import org.zstack.pciDevice.entity.PciDeviceVO
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecVO
import org.zstack.pciDevice.entity.specification.mdev.PciDeviceMdevSpecRefVO
import org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecVO
import org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceVO
import org.zstack.testlib.dbremaining.AllowedDBRemaining

class PciDeviceDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = PciDeviceSpecVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = MdevDeviceSpecVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = PciDeviceMdevSpecRefVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                return vos.findAll { ![
                        PciDeviceVO.class.simpleName
                        , MdevDeviceVO.class.simpleName
                        , PciDeviceSpecVO.class.simpleName
                        , MdevDeviceSpecVO.class.simpleName
                        , HostNetworkBondingVO.class.simpleName
                        , HostNetworkInterfaceVO.class.simpleName
                ].contains(it.getResourceType()) }
            }
        }
    }
}