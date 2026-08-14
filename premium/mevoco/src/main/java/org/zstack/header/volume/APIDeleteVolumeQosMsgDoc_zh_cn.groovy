package org.zstack.header.volume

import org.zstack.header.volume.APIDeleteVolumeQosEvent

doc {
	title "DeleteVolumeQos"

	category "mevoco"

	desc """取消云盘限速"""

	rest {
		request {
			url "DELETE /v1/volumes/{uuid}/qos"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVolumeQosMsg.class

			desc """取消云盘限速"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "云盘的UUID"
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
				column {
					name "mode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.1.0"
					values ("total","read","write","all","overwrite")
				}
			}
		}

		response {
			clz APIDeleteVolumeQosEvent.class
		}
	}
}