package org.zstack.header.baremetal.pxeserver

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory

doc {

	title "部署服务器挂载集群返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.pxeserver.APIAttachBaremetalPxeServerToClusterEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.1"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.baremetal.pxeserver.APIAttachBaremetalPxeServerToClusterEvent.inventory"
		desc "部署服务器清单"
		type "BaremetalPxeServerInventory"
		since "3.1.1"
		clz BaremetalPxeServerInventory.class
	}
}
