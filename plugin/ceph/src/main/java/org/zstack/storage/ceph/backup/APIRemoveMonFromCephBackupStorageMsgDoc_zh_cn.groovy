package org.zstack.storage.ceph.backup

import org.zstack.storage.ceph.backup.APIRemoveMonFromCephBackupStorageEvent

doc {
	title "从 Ceph 镜像服务器删除 mon 节点(RemoveMonFromCephBackupStorage)"

	category "storage.ceph.backup"

	desc """从 Ceph 镜像服务器删除 mon 节点"""

	rest {
		request {
			url "DELETE /v1/backup-storage/ceph/{uuid}/mons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveMonFromCephBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "Ceph 镜像服务器的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "monHostnames"
					enclosedIn ""
					desc "mon 节点名字列表"
					location "query"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRemoveMonFromCephBackupStorageEvent.class
		}
	}
}