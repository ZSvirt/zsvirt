package org.zstack.storage.ceph.primary

import org.zstack.storage.ceph.primary.APIDeleteCephPrimaryStoragePoolEvent

doc {
	title "DeleteCephPrimaryStoragePool"

	category "storage.ceph.primary"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/primary-storage/ceph/pools/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteCephPrimaryStoragePoolMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteCephPrimaryStoragePoolEvent.class
		}
	}
}