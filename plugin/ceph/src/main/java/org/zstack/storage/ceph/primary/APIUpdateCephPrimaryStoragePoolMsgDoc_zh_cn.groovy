package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIUpdateCephPrimaryStoragePoolEvent

doc {
	title "UpdateCephPrimaryStoragePool"

	category "storage.ceph.primary"

	desc """Ceph Pool存储池清单"""

	rest {
		request {
			url "PUT /v1/primary-storage/ceph/pools/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateCephPrimaryStoragePoolMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateCephPrimaryStoragePool"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "aliasName"
					enclosedIn "updateCephPrimaryStoragePool"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "updateCephPrimaryStoragePool"
					desc "资源的详细描述"
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
			}
		}

		response {
			clz APIUpdateCephPrimaryStoragePoolEvent.class
		}
	}
}