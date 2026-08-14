package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIUpdateCephPrimaryStorageMonEvent

doc {
	title "更新 Ceph 主存储 mon 节点(UpdateCephPrimaryStorageMon)"

	category "storage.ceph.primary"

	desc """更新 Ceph 主存储 mon 节点"""

	rest {
		request {
			url "PUT /v1/primary-storage/ceph/mons/{monUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateCephPrimaryStorageMonMsg.class

			desc """"""

			params {

				column {
					name "monUuid"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 节点UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "hostname"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 节点新主机名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshUsername"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 节点主机 ssh 用户名"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPassword"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 节点主机 ssh 用户密码"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "sshPort"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 节点主机 ssh 端口"
					location "body"
					type "Integer"
					optional true
					since "0.6"
				}
				column {
					name "monPort"
					enclosedIn "updateCephPrimaryStorageMon"
					desc "mon 的新端口"
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
			clz APIUpdateCephPrimaryStorageMonEvent.class
		}
	}
}