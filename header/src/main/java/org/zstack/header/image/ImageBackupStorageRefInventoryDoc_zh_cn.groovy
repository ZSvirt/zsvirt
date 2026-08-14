package org.zstack.header.image

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "backupStorageUuid"
		desc "镜像存储UUID"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "installPath"
		desc "在镜像服务器上的安装路径"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "exportUrl"
		desc "导出镜像的url"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "exportMd5Sum"
		desc "导出镜像的md5值"
		type "String"
		since "zsv 3.9.0"
	}
	field {
		name "status"
		desc "镜像就绪状态"
		type "String"
		since "zsv 0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 0.6"
	}
}
