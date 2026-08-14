package org.zstack.header.storage.addon.primary

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "AddExternalPrimaryStorage"

	category "storage.primary"

	desc """添加外部存储"""

	rest {
		request {
			url "POST /v1/primary-storage/addon"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddExternalPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "identity"
					enclosedIn "params"
					desc "存储标识"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "defaultOutputProtocol"
					enclosedIn "params"
					desc "默认输出协议"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
					values ("Vhost","Scsi","Nvme","CBD","file")
				}
				column {
					name "config"
					enclosedIn "params"
					desc "配置"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.6"
				}
			}
		}

		response {
			clz APIAddPrimaryStorageEvent.class
		}
	}
}