package org.zstack.storage.ceph.backup

import org.zstack.storage.ceph.backup.APIUpdateCephBackupStorageMonEvent

doc {
	title "更新 Ceph 镜像服务器 mon 节点(UpdateCephBackupStorageMon)"

	category "storage.ceph.backup"

	desc """更新 Ceph 镜像服务器 mon 节点"""

	rest {
		request {
			url "PUT /v1/backup-storage/ceph/mons/{monUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateCephBackupStorageMonMsg.class

			desc """"""

			params {

				column {
					name "monUuid"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "hostname"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点新主机地址"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshUsername"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点主机 ssh 用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPassword"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点主机 ssh 用户密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点主机 ssh 端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "monPort"
					enclosedIn "updateCephBackupStorageMon"
					desc "mon 节点的端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateCephBackupStorageMonEvent.class
		}
	}
}