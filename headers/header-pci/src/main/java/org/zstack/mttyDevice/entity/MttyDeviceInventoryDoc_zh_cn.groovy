package org.zstack.mttyDevice.entity

import org.zstack.mttyDevice.header.MttyDeviceType
import org.zstack.mttyDevice.header.MttyDeviceState
import org.zstack.mttyDevice.header.MttyDeviceVirtStatus
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "MTTY 设备清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv_3.15.11"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv_3.15.11"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv_3.15.11"
	}
	field {
		name "hostUuid"
		desc "主机 UUID"
		type "String"
		since "zsv_3.15.11"
	}
	ref {
		name "type"
		path "org.zstack.mttyDevice.entity.MttyDeviceInventory.type"
		desc "MTTY 设备类型"
		type "MttyDeviceType"
		since "zsv_3.15.11"
		clz MttyDeviceType.class
	}
	ref {
		name "state"
		path "org.zstack.mttyDevice.entity.MttyDeviceInventory.state"
		desc "MTTY 设备启用状态"
		type "MttyDeviceState"
		since "zsv_3.15.11"
		clz MttyDeviceState.class
	}
	ref {
		name "virtStatus"
		path "org.zstack.mttyDevice.entity.MttyDeviceInventory.virtStatus"
		desc "MTTY 设备虚拟类型"
		type "MttyDeviceVirtStatus"
		since "zsv_3.15.11"
		clz MttyDeviceVirtStatus.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_3.15.11"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_3.15.11"
	}
}
