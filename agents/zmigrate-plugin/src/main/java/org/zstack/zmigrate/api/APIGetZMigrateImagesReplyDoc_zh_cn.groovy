package org.zstack.zmigrate.api

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取ZMigrate网关镜像返回"

	ref {
		name "images"
		path "org.zstack.zmigrate.api.APIGetZMigrateImagesReply.images"
		desc "获取ZMigrate网关镜像列表"
		type "Map"
		since "5.0.0"
		clz ImageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.zmigrate.api.APIGetZMigrateImagesReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
}
