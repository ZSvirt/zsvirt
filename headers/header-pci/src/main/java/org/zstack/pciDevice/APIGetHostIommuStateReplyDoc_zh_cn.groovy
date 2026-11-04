package org.zstack.pciDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.header.HostIommuStateType
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机IOMMU启用状态回复"

	ref {
		name "error"
		path "org.zstack.pciDevice.APIGetHostIommuStateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_2.1"
		clz ErrorCode.class
	}
	ref {
		name "state"
		path "org.zstack.pciDevice.APIGetHostIommuStateReply.state"
		desc "获取物理机IOMMU启用状态"
		type "HostIommuStateType"
		since "zsv_2.1"
		clz HostIommuStateType.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_2.1"
	}
}
