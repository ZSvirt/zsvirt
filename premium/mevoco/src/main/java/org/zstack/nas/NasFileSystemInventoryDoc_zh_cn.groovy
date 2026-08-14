package org.zstack.nas

import org.zstack.nas.NasProtocolType
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "nas文件系统清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.4.0"
	}
	ref {
		name "protocol"
		path "org.zstack.nas.NasFileSystemInventory.protocol"
		desc "nas协议类型"
		type "NasProtocolType"
		since "2.4.0"
		clz NasProtocolType.class
	}
	field {
		name "type"
		desc "nas类型"
		type "String"
		since "2.4.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.4.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.4.0"
	}
	field {
		name "fileSystemId"
		desc "文件系统id"
		type "String"
		since "2.4.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.4.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.4.0"
	}
}
