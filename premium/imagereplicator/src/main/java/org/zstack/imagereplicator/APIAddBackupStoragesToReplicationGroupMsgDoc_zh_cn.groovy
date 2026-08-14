package org.zstack.imagereplicator

import org.zstack.imagereplicator.APIAddBackupStoragesToReplicationGroupEvent

doc {
	title "AddBackupStoragesToReplicationGroup"

	category "imagereplicator"

	desc """添加镜像服务器到镜像复制组"""

	rest {
		request {
			url "POST /v1/image-replication-groups/{replicationGroupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddBackupStoragesToReplicationGroupMsg.class

			desc """"""

			params {

				column {
					name "replicationGroupUuid"
					enclosedIn "params"
					desc "镜像复制组UUID"
					location "url"
					type "String"
					optional false
					since "3.5"
				}
				column {
					name "backupStorageUuids"
					enclosedIn "params"
					desc "镜像服务器列表"
					location "body"
					type "List"
					optional false
					since "3.5"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.5"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.5"
				}
			}
		}

		response {
			clz APIAddBackupStoragesToReplicationGroupEvent.class
		}
	}
}