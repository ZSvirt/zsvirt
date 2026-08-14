package org.zstack.header.tpm.api

import org.zstack.header.tpm.entity.TpmCapabilityView
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取 TPM 详情数据的结果"

	ref {
		name "inventory"
		path "org.zstack.header.tpm.api.APIGetTpmCapabilityReply.inventory"
		desc "TPM 性能和信息数据"
		type "TpmCapabilityView"
		since "zsv 5.0.0"
		clz TpmCapabilityView.class
	}
	field {
		name "success"
		desc "获取是否成功"
		type "boolean"
		since "zsv 5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.header.tpm.api.APIGetTpmCapabilityReply.error"
		desc "错误码，若不为 null，则表示操作失败, 操作成功时该字段为 null"
		type "ErrorCode"
		since "zsv 5.0.0"
		clz ErrorCode.class
	}
}
