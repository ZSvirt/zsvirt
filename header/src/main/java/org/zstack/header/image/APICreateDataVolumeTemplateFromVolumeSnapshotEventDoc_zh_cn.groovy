package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotEvent.Failure
import org.zstack.header.image.ImageInventory

doc {

	title "镜像清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "zsv 0.6"
		clz ImageInventory.class
	}
	ref {
		name "failures"
		path "org.zstack.header.image.APICreateDataVolumeTemplateFromVolumeSnapshotEvent.failuresOnBackupStorage"
		desc "null"
		type "List"
		since "zsv 0.6"
		clz Failure.class
	}
}
