package org.zstack.header.core.external.service

import org.zstack.header.errorcode.ErrorCode

doc {

	title "删除外部服务配置返回"

	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "1.0.1, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIDeleteExternalServiceConfigurationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.1, zsv_5.1.0"
		clz ErrorCode.class
	}
}
