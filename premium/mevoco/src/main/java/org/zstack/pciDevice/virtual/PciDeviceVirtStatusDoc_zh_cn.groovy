package org.zstack.pciDevice.virtual



doc {

	title "PCI设备虚拟状态"

	field {
		name "UNVIRTUALIZABLE"
		desc "不可虚拟化"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "SRIOV_VIRTUALIZABLE"
		desc "支持SRIOV虚拟化"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "VFIO_MDEV_VIRTUALIZABLE"
		desc "支持VFIO_MDEV虚拟化"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "SRIOV_VIRTUALIZED"
		desc "已SRIOV虚拟化"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "VFIO_MDEV_VIRTUALIZED"
		desc "已VFIO_MDEV虚拟化"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "SRIOV_VIRTUAL"
		desc "SRIOV虚拟设备"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
	field {
		name "UNKNOWN"
		desc "未知"
		type "PciDeviceVirtStatus"
		since "3.5.0"
	}
}
