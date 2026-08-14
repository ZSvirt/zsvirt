package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.ExternalServiceCapabilities

doc {

	title "External Service的数据结构"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 3.17.0"
	}
	field {
		name "status"
		desc "服务状态"
		type "String"
		since "zsv 3.17.0"
	}
	ref {
		name "capabilities"
		path "org.zstack.header.core.external.service.ExternalServiceInventory.capabilities"
		desc "External Service的功能描述"
		type "ExternalServiceCapabilities"
		since "zsv 3.17.0"
		clz ExternalServiceCapabilities.class
	}
}
