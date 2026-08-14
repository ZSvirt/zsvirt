package org.zstack.header.storage.primary

doc {

	title "在这里输入结构的名称"

	field {
		name "id"
		desc ""
		type "long"
		since "zsv 2.3.2.1"
	}
	field {
		name "primaryStorageUuid"
		desc "主存储UUID"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "installUrl"
		desc "镜像路径"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "mediaType"
		desc "镜像类型，RootVolumeTemplate、DataVolumeTemplate或ISO"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "size"
		desc "镜像大小"
		type "long"
		since "zsv 2.3.2.1"
	}
	field {
		name "md5sum"
		desc "镜像的md5"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "state"
		desc "镜像缓存的状态"
		type "String"
		since "zsv 2.3.2.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 2.3.2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 2.3.2.1"
	}
}
