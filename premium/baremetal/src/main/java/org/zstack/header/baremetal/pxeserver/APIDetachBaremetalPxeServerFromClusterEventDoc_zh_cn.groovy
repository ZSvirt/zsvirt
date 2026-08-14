package org.zstack.header.baremetal.pxeserver

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory

doc {

	title "从裸金属集群卸载部署服务器返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.pxeserver.APIDetachBaremetalPxeServerFromClusterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.baremetal.pxeserver.APIDetachBaremetalPxeServerFromClusterEvent.inventory"
		desc "部署服务器清单"
		type "BaremetalPxeServerInventory"
		since "3.1.1"
		clz BaremetalPxeServerInventory.class
	}
}
