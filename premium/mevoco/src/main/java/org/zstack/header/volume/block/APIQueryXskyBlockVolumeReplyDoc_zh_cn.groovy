package org.zstack.header.volume.block

import org.zstack.header.volume.block.XskyBlockVolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "查询 XSky 类型块存储卷的请求返回"

	ref {
		name "inventories"
		path "org.zstack.header.volume.block.APIQueryXskyBlockVolumeReply.inventories"
		desc "XSky 类型块存储卷清单列表"
		type "List"
		since "3.17.11"
		clz XskyBlockVolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.block.APIQueryXskyBlockVolumeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
