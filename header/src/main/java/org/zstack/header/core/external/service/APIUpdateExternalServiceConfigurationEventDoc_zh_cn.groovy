package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.ExternalServiceConfigurationInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新外部服务配置"

	ref {
		name "inventory"
		path "org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent.inventory"
		desc "null"
		type "ExternalServiceConfigurationInventory"
		since "1.0.0, zsv_5.1.0"
		clz ExternalServiceConfigurationInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "1.0.0, zsv_5.1.0"
		clz ErrorCode.class
	}
}
