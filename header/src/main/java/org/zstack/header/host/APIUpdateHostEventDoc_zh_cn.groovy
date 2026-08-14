package org.zstack.header.host

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.host.HostInventory

doc {

	title "更新主机的请求返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 3.17.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.host.APIUpdateHostEvent.inventory"
		desc "更新后的主机信息"
		type "HostInventory"
		since "zsv 3.17.0"
		clz HostInventory.class
	}
}
