package org.zstack.softwarePackage.header

doc {

	title "软件包信息"

	field {
		name "uuid"
		desc "软件包 UUID"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "name"
		desc "软件包名称"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "hostUuid"
		desc "软件包所在的主机 UUID"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "managementNodeUuid"
		desc "软件包所在的管理节点 UUID"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "installPath"
		desc "软件包安装路径"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "unzipInstallPath"
		desc "软件包解压路径"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "type"
		desc "软件包类型"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "md5sum"
		desc "软件包 MD5 sum"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "status"
		desc "软件包状态"
		type "String"
		since "zsv_4.10.20"
	}
	field {
		name "size"
		desc "软件包大小"
		type "Long"
		since "zsv_4.10.20"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_4.10.20"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_4.10.20"
	}
}