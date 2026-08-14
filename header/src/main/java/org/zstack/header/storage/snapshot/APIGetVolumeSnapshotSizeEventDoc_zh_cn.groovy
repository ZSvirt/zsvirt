package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode
import java.lang.Long
import java.lang.Long
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取快照容量返回"

	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIGetVolumeSnapshotSizeEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 3.5"
		clz ErrorCode.class
	}
	field {
		name "size"
		desc "快照容量"
		type "Long"
		since "zsv 3.5"
	}
	field {
		name "actualSize"
		desc "快照实际容量"
		type "Long"
		since "zsv 3.5"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 3.5"
	}
}
