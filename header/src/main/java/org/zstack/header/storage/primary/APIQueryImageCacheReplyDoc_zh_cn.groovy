package org.zstack.header.storage.primary

import org.zstack.header.errorcode.ErrorCode

doc {

	title "镜像缓存清单列表"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.primary.APIQueryImageCacheReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.3.2.1"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.primary.APIQueryImageCacheReply.inventories"
		desc "镜像缓存清单"
		type "List"
		since "zsv 2.3.2.1"
		clz ImageCacheInventory.class
	}
}
