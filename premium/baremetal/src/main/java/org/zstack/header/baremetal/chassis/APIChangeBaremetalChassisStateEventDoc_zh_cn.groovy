package org.zstack.header.baremetal.chassis

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.chassis.BaremetalChassisInventory

doc {

	title "修改裸机设备状态返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.chassis.APIChangeBaremetalChassisStateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.baremetal.chassis.APIChangeBaremetalChassisStateEvent.inventory"
		desc "裸机设备清单"
		type "BaremetalChassisInventory"
		since "2.6.0"
		clz BaremetalChassisInventory.class
	}
}
