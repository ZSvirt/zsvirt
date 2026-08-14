package org.zstack.imagereplicator

import org.zstack.header.errorcode.ErrorCode
import org.zstack.imagereplicator.ImageReplicationGroupInventory

doc {

	title "查询镜像复制组返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.imagereplicator.APIQueryImageReplicationGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.5"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.imagereplicator.APIQueryImageReplicationGroupReply.inventories"
		desc "null"
		type "List"
		since "3.5"
		clz ImageReplicationGroupInventory.class
	}
}
