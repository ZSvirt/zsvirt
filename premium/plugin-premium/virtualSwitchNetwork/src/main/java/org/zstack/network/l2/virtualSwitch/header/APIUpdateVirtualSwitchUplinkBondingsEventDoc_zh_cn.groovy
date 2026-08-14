package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新交换机上行链路绑定配置结果"

	ref {
		name "inventories"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkBondingsEvent.inventories"
		desc "绑定结果列表"
		type "List"
		since "3.18.0"
		clz HostNetworkBondingInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.18.0"
	}
	ref {
		name "error"
		path "org.zstack.network.l2.virtualSwitch.header.APIUpdateVirtualSwitchUplinkBondingsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.18.0"
		clz ErrorCode.class
	}
}
