package org.zstack.header.baremetal.preconfiguration

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.baremetal.preconfiguration.PreconfigurationTemplateInventory

doc {

	title "查询预配置模板返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.baremetal.preconfiguration.APIQueryPreconfigurationTemplatesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.baremetal.preconfiguration.APIQueryPreconfigurationTemplatesReply.inventories"
		desc "预配置模板清单"
		type "List"
		since "3.4.0"
		clz PreconfigurationTemplateInventory.class
	}
}
