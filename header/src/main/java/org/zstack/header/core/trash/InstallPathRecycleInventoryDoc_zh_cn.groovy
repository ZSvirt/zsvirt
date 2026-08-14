package org.zstack.header.core.trash

doc {

	title "回收数据清单"

	field {
		name "trashId"
		desc "回收数据的Id"
		type "long"
		since "zsv 3.7.0"
	}
	field {
		name "resourceUuid"
		desc "回收数据的资源Uuid"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "storageUuid"
		desc "回收数据的存储Uuid"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "storageType"
		desc "回收数据的存储类型"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "resourceType"
		desc "回收数据的资源回型"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "installPath"
		desc "回收数据的路径"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "hostUuid"
		desc "回收数据原来所在的Host(LocalStorage)"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "isFolder"
		desc "清理时是否以目录为单位"
		type "Boolean"
		since "zsv 3.7.0"
	}
	field {
		name "hypervisorType"
		desc "虚拟化类型"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "size"
		desc "回收数据的大小"
		type "Long"
		since "zsv 3.7.0"
	}
	field {
		name "trashType"
		desc "回收数据的来源"
		type "String"
		since "zsv 3.7.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 3.7.0"
	}
}
