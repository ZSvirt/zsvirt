package org.zstack.header.tpm.api

import org.zstack.header.tpm.entity.TpmInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "虚拟机添加 TPM 的结果"

	ref {
		name "inventory"
		path "org.zstack.header.tpm.api.APIAddTpmEvent.inventory"
		desc "TPM 信息"
		type "TpmInventory"
		since "zsv 5.0.0"
		clz TpmInventory.class
	}
	field {
		name "success"
		desc "添加是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.tpm.api.APIAddTpmEvent.error"
		desc "错误码，若不为 null，则表示操作失败, 操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
