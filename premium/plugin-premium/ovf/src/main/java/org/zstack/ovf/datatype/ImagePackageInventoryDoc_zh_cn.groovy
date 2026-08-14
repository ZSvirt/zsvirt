package org.zstack.ovf.datatype

import org.zstack.ovf.datatype.ImagePackageState
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "镜像包"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.14.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.14.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.14.6"
	}
	field {
		name "vmUuid"
		desc "源虚拟机UUID"
		type "String"
		since "3.14.6"
	}
	field {
		name "backupStorageUuid"
		desc "镜像包文件位于的镜像存储UUID"
		type "String"
		since "3.14.6"
	}
	ref {
		name "state"
		path "org.zstack.ovf.datatype.ImagePackageInventory.state"
		desc "镜像包状态"
		type "ImagePackageState"
		since "3.14.6"
		clz ImagePackageState.class
	}
	field {
		name "exportUrl"
		desc "下载地址"
		type "String"
		since "3.14.6"
	}
	field {
		name "md5Sum"
		desc "镜像包文件MD5校验码"
		type "String"
		since "3.14.6"
	}
	field {
		name "format"
		desc "镜像包文件格式"
		type "String"
		since "3.14.6"
	}
	field {
		name "size"
		desc "镜像包文件大小"
		type "Long"
		since "3.14.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.14.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.14.6"
	}
}
