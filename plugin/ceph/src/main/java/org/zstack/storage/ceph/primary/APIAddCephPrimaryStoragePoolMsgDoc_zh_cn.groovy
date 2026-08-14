package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIAddCephPrimaryStoragePoolEvent

doc {
	title "AddCephPrimaryStoragePool"

	category "storage.ceph.primary"

	desc """为Ceph主存储添加Pool"""

	rest {
		request {
			url "POST /v1/primary-storage/ceph/{primaryStorageUuid}/pools"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddCephPrimaryStoragePoolMsg.class

			desc """"""

			params {

				column {
					name "primaryStorageUuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "poolName"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "aliasName"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "isCreate"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "存储池类型"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("Root","Data")
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddCephPrimaryStoragePoolEvent.class
		}
	}
}