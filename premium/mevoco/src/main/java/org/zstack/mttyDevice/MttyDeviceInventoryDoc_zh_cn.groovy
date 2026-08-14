package org.zstack.mttyDevice

import org.zstack.mttyDevice.MttyDeviceType
import org.zstack.mttyDevice.MttyDeviceState
import org.zstack.mttyDevice.MttyDeviceVirtStatus
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "MTTY 设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.15.11"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.15.11"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.15.11"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "3.15.11"
	}
	ref {
		name "type"
		path "org.zstack.mttyDevice.MttyDeviceInventory.type"
		desc "MTTY 设备类型"
		type "MttyDeviceType"
		since "3.15.11"
		clz MttyDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.mttyDevice.MttyDeviceInventory.state"
		desc "MTTY 设备启用状态"
		type "MttyDeviceState"
		since "3.15.11"
		clz MttyDeviceState.class
	}
	ref {
		name "virtStatus"
		path "org.zstack.mttyDevice.MttyDeviceInventory.virtStatus"
		desc "MTTY 设备虚拟类型"
		type "MttyDeviceVirtStatus"
		since "3.15.11"
		clz MttyDeviceVirtStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.15.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.15.11"
	}
}
