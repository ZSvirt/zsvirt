package org.zstack.storage.backup.imagestore

import org.zstack.header.errorcode.ErrorCode

doc {

	title "镜像仓库中的原始数据清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.storage.backup.imagestore.APIGetImagesFromImageStoreBackupStorageReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.2"
		clz ErrorCode.class
	}
	ref {
		name "infos"
		path "org.zstack.storage.backup.imagestore.APIGetImagesFromImageStoreBackupStorageReply.infos"
		desc "null"
		type "List"
		since "2.2"
		clz ImageStoreImageStruct.class
	}
}
