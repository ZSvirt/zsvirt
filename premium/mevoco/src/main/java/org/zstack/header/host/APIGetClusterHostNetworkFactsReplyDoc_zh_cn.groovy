package org.zstack.header.host


import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取集群主机物理网络信息清单的请求返回"

	ref {
		name "error"
		path "org.zstack.header.host.APIGetClusterHostNetworkFactsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.13.6"
		clz ErrorCode.class
	}
	ref {
		name "bondings"
		path "org.zstack.header.host.APIGetClusterHostNetworkFactsReply.bondings"
		desc "Bond 设备清单列表"
		type "List"
		since "3.13.6"
		clz HostNetworkBondingInventory.class
	}
	ref {
		name "nics"
		path "org.zstack.header.host.APIGetClusterHostNetworkFactsReply.nics"
		desc "网卡设备清单列表"
		type "List"
		since "3.13.6"
		clz HostNetworkInterfaceInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.13.6"
	}
}
