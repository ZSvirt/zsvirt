package org.zstack.pciDevice.specification.mdev

import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecState
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType
import org.zstack.pciDevice.specification.mdev.MdevDeviceSpecState
import java.sql.Timestamp

doc {

	title "MDEV设备规格清单"

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
		name "specification"
		desc "规格详情"
		type "String"
		since "3.5.0"
	}
	ref {
		name "type"
		path "org.zstack.pciDevice.specification.mdev.MdevDeviceSpecInventory.type"
		desc "MDEV设备类型"
		type "MdevDeviceType"
		since "3.5.0"
		clz MdevDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.specification.mdev.MdevDeviceSpecInventory.state"
		desc "规格启用状态"
		type "MdevDeviceSpecState"
		since "3.5.0"
		clz MdevDeviceSpecState.class
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
