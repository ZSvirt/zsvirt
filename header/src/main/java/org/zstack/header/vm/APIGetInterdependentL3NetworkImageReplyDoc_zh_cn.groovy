package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode

doc {

	title "三层网络、镜像清单"

	ref {
		name "error"
		path "org.zstack.header.vm.APIGetInterdependentL3NetworkImageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	field {
		name "inventories"
		desc "若输入参数是`l3NetworkUuids`，为镜像清单;若输入参数是`imageUuid`，为三层网络清单"
		type "List"
		since "zsv 0.6"
	}
}
