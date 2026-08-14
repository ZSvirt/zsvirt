package org.zstack.header.baremetal.chassis

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.longjob.LongJobInventory

doc {

	title "批量添加裸金属设备长任务返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.chassis.APIBatchCreateBaremetalChassisEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.baremetal.chassis.APIBatchCreateBaremetalChassisEvent.inventory"
		desc "批量添加裸金属设备长任务清单"
		type "LongJobInventory"
		since "3.1.1"
		clz LongJobInventory.class
	}
}
