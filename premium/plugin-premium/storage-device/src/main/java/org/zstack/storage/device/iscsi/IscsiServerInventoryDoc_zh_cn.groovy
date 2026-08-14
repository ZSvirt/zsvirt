package org.zstack.storage.device.iscsi

import java.lang.Integer
import org.zstack.storage.device.iscsi.IscsiTargetInventory
import org.zstack.storage.device.iscsi.IscsiServerClusterRefInventory
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "iSCSI服务器清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.0.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.0.0"
	}
	field {
		name "ip"
		desc "IP地址"
		type "String"
		since "3.0.0"
	}
	field {
		name "port"
		desc "端口"
		type "Integer"
		since "3.0.0"
	}
	field {
		name "chapUserName"
		desc "CHAP用户名"
		type "String"
		since "3.0.0"
	}
	field {
		name "chapUserPassword"
		desc "CHAP密码"
		type "String"
		since "3.0.0"
	}
	field {
		name "state"
		desc "启用状态"
		type "String"
		since "3.0.0"
	}
	ref {
		name "iscsiTargets"
		path "org.zstack.storage.device.iscsi.IscsiServerInventory.iscsiTargets"
		desc "iSCSI目标"
		type "List"
		since "3.0.0"
		clz IscsiTargetInventory.class
	}
	ref {
		name "iscsiClusterRefs"
		path "org.zstack.storage.device.iscsi.IscsiServerInventory.iscsiClusterRefs"
		desc "iSCSI服务器加载的集群"
		type "List"
		since "3.0.0"
		clz IscsiServerClusterRefInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.0.0"
	}
}
