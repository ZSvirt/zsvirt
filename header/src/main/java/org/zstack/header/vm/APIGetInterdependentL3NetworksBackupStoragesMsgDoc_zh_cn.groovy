package org.zstack.header.vm

import org.zstack.header.vm.APIGetInterdependentL3NetworksBackupStoragesReply

doc {
	title "GetInterdependentL3NetworksBackupStorages"

	category "vmInstance"

	desc """获取内部依赖的L3网络或者镜像服务器信息"""

	rest {
		request {
			url "GET /v1/backupStorage-l3networks/dependencies"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetInterdependentL3NetworksBackupStoragesMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn ""
					desc "镜像存储UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetInterdependentL3NetworksBackupStoragesReply.class
		}
	}
}