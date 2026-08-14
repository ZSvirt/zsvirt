package org.zstack.storage.primary.block.message

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "AddBlockPrimaryStorage"

	category "storage.primary"

	desc """添加block主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/block"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddBlockPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "vendorName"
					enclosedIn "param"
					desc "存储厂商名称"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "metadata"
					enclosedIn "param"
					desc "存储的元数据"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "url"
					enclosedIn "param"
					desc "未使用"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "name"
					enclosedIn "param"
					desc "Block主存储的名称"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "description"
					enclosedIn "param"
					desc "Block主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "type"
					enclosedIn "param"
					desc "主存储的类型，此处为Block"
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "zoneUuid"
					enclosedIn "param"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "resourceUuid"
					enclosedIn "param"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "tagUuids"
					enclosedIn "param"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "encryptGatewayIp"
					enclosedIn "param"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "encryptGatewayPort"
					enclosedIn "param"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "3.15.11"
				}
				column {
					name "encryptGatewayUsername"
					enclosedIn "param"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "encryptGatewayPassword"
					enclosedIn "param"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.15.11"
				}
			}
		}

		response {
			clz APIAddPrimaryStorageEvent.class
		}
	}
}