package org.zstack.pciDevice

import org.zstack.header.errorcode.ErrorCode
import org.zstack.pciDevice.HostIommuStatusType
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机IOMMU就绪状态回复"

	ref {
		name "error"
		path "org.zstack.pciDevice.APIGetHostIommuStatusReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
	ref {
		name "status"
		path "org.zstack.pciDevice.APIGetHostIommuStatusReply.status"
		desc "获取物理机IOMMU就绪状态"
		type "HostIommuStatusType"
		since "2.1"
		clz HostIommuStatusType.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "2.1"
	}
}
