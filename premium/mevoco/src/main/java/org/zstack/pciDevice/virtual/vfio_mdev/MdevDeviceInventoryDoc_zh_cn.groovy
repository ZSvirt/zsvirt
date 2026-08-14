package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceState
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceStatus
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceChooser
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "MDEV设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.5.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.5.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.5.0"
	}
	field {
		name "parentUuid"
		desc "物理PCI设备UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "vmInstanceUuid"
		desc "虚拟机 UUID"
		type "String"
		since "3.5.0"
	}
	field {
		name "mdevSpecUuid"
		desc "MDEV设备规格UUID"
		type "String"
		since "3.5.0"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceInventory.type"
		desc "MDEV设备类型"
		type "MdevDeviceType"
		since "3.5.0"
		clz MdevDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceInventory.state"
		desc "MDEV设备启用状态"
		type "MdevDeviceState"
		since "3.5.0"
		clz MdevDeviceState.class
	}
	ref {
		name "status"
		path "org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceInventory.status"
		desc "MDEV设备挂载状态"
		type "MdevDeviceStatus"
		since "3.5.0"
		clz MdevDeviceStatus.class
	}
	ref {
		name "chooser"
		path "org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceInventory.chooser"
		desc "MDEV设备选取者"
		type "MdevDeviceChooser"
		since "3.11.0"
		clz MdevDeviceChooser.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.5.0"
	}
}
