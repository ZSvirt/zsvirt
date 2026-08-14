package org.zstack.header.vm

import java.lang.Integer
import java.lang.Integer
import java.lang.Integer

doc {

	title "vdi端口号信息"

	field {
		name "vncPort"
		desc "vnc端口号"
		type "Integer"
		since "zsv 3.7"
	}
	field {
		name "spicePort"
		desc "spice端口号"
		type "Integer"
		since "zsv 3.7"
	}
	field {
		name "spiceTlsPort"
		desc "spice开启Tls加密，会存在spiceTlsPort和spicePort两个端口号，通过spice客户端连接云主机需要使用spiceTlsPort端口号"
		type "Integer"
		since "zsv 3.7"
	}
}
