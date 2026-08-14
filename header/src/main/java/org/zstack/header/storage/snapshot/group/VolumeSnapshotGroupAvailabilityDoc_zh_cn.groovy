package org.zstack.header.storage.snapshot.group



doc {

	title "云盘快照组可用性"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 3.6.0"
	}
	field {
		name "available"
		desc "是否可以恢复"
		type "boolean"
		since "zsv 3.6.0"
	}
	field {
		name "reason"
		desc "不可恢复的理由，如可恢复则为空"
		type "String"
		since "zsv 3.6.0"
	}
}
