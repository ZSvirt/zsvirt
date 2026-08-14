package org.zstack.header.network.l3

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取三层网络上路由器接口地址回复"

	ref {
		name "error"
		path "org.zstack.header.network.l3.APIGetL3NetworkRouterInterfaceIpReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.2"
		clz ErrorCode.class
	}
	field {
		name "routerInterfaceIp"
		desc "三层网络上路由器的接口地址，仅当会在普通三层网络上创建云路由器或在VPC网络上加载VPC路由器时有效"
		type "String"
		since "zsv 2.2"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 2.2"
	}
}
