package org.zstack.header.image

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取镜像 MD5 值返回"

	ref {
		name "inventory"
		path "org.zstack.header.image.APICalculateImageHashEvent.inventory"
		desc "镜像清单"
		type "ImageInventory"
		since "zsv 4.1.0"
		clz ImageInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "zsv 4.1.0"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APICalculateImageHashEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv 4.1.0"
		clz ErrorCode.class
	}
}
