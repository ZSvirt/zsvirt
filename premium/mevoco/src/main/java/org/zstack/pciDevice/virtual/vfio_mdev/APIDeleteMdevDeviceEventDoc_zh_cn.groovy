package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.header.errorcode.ErrorCode

doc {

	title "MDEV 设备删除结果"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.15.11"
	}
	ref {
		name "error"
		path "org.zstack.pciDevice.virtual.vfio_mdev.APIDeleteMdevDeviceEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.15.11"
		clz ErrorCode.class
	}
}
