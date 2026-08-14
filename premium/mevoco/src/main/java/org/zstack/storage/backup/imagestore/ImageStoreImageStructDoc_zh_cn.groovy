package org.zstack.storage.backup.imagestore

import java.sql.Timestamp
import java.lang.Long
import java.lang.Long

doc {

	title "镜像仓库中的镜像属性"

	field {
		name "id"
		desc "镜像仓库中的id"
		type "String"
		since "2.2"
	}
	field {
		name "parent"
		desc "父镜像名称"
		type "String"
		since "2.2"
	}
	field {
		name "blobsum"
		desc "镜像hash码"
		type "String"
		since "2.2"
	}
	field {
		name "created"
		desc "镜像提交时间"
		type "Timestamp"
		since "2.2"
	}
	field {
		name "author"
		desc "镜像制作者"
		type "String"
		since "2.2"
	}
	field {
		name "arch"
		desc "镜像的os结构"
		type "String"
		since "2.2"
	}
	field {
		name "desc"
		desc "镜像描述"
		type "String"
		since "2.2"
	}
	field {
		name "size"
		desc "镜像的真实大小"
		type "Long"
		since "2.2"
	}
	field {
		name "virtualsize"
		desc "镜像的虚拟大小"
		type "Long"
		since "2.2"
	}
	field {
		name "name"
		desc "镜像仓库中的镜像名称"
		type "String"
		since "2.2"
	}
}
