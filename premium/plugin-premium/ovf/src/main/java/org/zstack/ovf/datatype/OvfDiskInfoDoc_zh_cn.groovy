package org.zstack.ovf.datatype

import java.lang.Long
import java.lang.Long

doc {

	title "OVF模板信息——磁盘"

	field {
		name "index"
		desc "磁盘序号"
		type "int"
		since "3.14.6"
	}
	field {
		name "diskId"
		desc "磁盘ID"
		type "String"
		since "3.14.6"
	}
	field {
		name "fileRef"
		desc "文件引用名称"
		type "String"
		since "3.14.6"
	}
	field {
		name "fileName"
		desc "镜像文件名"
		type "String"
		since "3.14.6"
	}
	field {
		name "format"
		desc "镜像文件格式"
		type "String"
		since "3.14.6"
	}
	field {
		name "populatedSize"
		desc "镜像文件大小"
		type "Long"
		since "3.14.6"
	}
	field {
		name "capacity"
		desc "磁盘容量，单位Byte"
		type "Long"
		since "3.14.6"
	}
}
