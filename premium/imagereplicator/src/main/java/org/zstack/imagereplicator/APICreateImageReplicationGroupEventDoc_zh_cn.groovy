package org.zstack.imagereplicator

import org.zstack.header.errorcode.ErrorCode
import org.zstack.imagereplicator.ImageReplicationGroupInventory

doc {

	title "创建镜像复制组结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.imagereplicator.APICreateImageReplicationGroupEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.imagereplicator.APICreateImageReplicationGroupEvent.inventory"
		desc "null"
		type "ImageReplicationGroupInventory"
		since "3.5"
		clz ImageReplicationGroupInventory.class
	}
}
