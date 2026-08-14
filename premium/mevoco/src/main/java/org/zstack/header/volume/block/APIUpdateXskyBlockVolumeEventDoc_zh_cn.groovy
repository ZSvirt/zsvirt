package org.zstack.header.volume.block

import org.zstack.header.volume.block.XskyBlockVolumeInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新 XSky 类型块存储卷的请求返回"

	ref {
		name "inventory"
		path "org.zstack.header.volume.block.APIUpdateXskyBlockVolumeEvent.inventory"
		desc "XSky 类型块存储卷清单"
		type "XskyBlockVolumeInventory"
		since "3.16.31"
		clz XskyBlockVolumeInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.16.31"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.block.APIUpdateXskyBlockVolumeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.16.31"
		clz ErrorCode.class
	}
}
