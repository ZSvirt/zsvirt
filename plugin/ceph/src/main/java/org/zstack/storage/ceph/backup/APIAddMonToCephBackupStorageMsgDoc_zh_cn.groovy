package org.zstack.storage.ceph.backup

import org.zstack.storage.ceph.backup.APIAddMonToCephBackupStorageEvent

doc {
	title "为 Ceph 镜像服务器添加 mon 节点(AddMonToCephBackupStorage)"

	category "storage.ceph.backup"

	desc """为 Ceph 镜像服务器添加 mon 节点"""

	rest {
		request {
			url "POST /v1/backup-storage/ceph/{uuid}/mons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddMonToCephBackupStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "Ceph 镜像服务器的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "monUrls"
					enclosedIn "params"
					desc "需要添加的 mon 节点地址列表"
					location "body"
					type "List"
					optional false
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
			clz APIAddMonToCephBackupStorageEvent.class
		}
	}
}