package org.zstack.nas

import org.zstack.header.errorcode.ErrorCode
import org.zstack.nas.NasMountTargetInventory

doc {

	title "NAS挂载点清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.nas.APIQueryNasMountTargetReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.4.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.nas.APIQueryNasMountTargetReply.inventories"
		desc "NAS挂载点清单"
		type "List"
		since "2.4.0"
		clz NasMountTargetInventory.class
	}
}
