package org.zstack.header.volume.block


import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取访问路径列表返回"

	ref {
		name "pathInfos"
		path "org.zstack.header.volume.block.APIGetAccessPathReply.pathInfos"
		desc "访问路径列表"
		type "List"
		since "3.17.11"
		clz AccessPathInfo.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.volume.block.APIGetAccessPathReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
