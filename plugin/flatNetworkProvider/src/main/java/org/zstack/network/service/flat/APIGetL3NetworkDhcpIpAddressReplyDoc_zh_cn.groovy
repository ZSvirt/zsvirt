package org.zstack.network.service.flat

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取DHCP服务所用地址返回值"

	ref {
		name "error"
		path "org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "ip"
		desc "IP地址"
		type "String"
		since "0.6"
	}
	field {
		name "ip6"
		desc "IPv6地址"
		type "String"
		since "3.10"
	}
	field {
		name "success"
		desc "成功"
		type "boolean"
		since "0.6"
	}
}
