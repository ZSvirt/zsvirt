package org.zstack.header.volume

import org.zstack.header.volume.APISetVolumeQosEvent

doc {
	title "SetVolumeQos"

	category "mevoco"

	desc """设置云盘限速"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVolumeQosMsg.class

			desc """设置云盘限速"""

			params {

				column {
					name "uuid"
					enclosedIn "setVolumeQos"
					desc "云盘的UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "volumeBandwidth"
					enclosedIn "setVolumeQos"
					desc "云盘限速带宽"
					location "body"
					type "Long"
					optional true
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "mode"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.1.0"
					values ("total","read","write")
				}
				column {
					name "readBandwidth"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
				column {
					name "writeBandwidth"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
				column {
					name "totalBandwidth"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
				column {
					name "readIOPS"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
				column {
					name "writeIOPS"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
				column {
					name "totalIOPS"
					enclosedIn "setVolumeQos"
					desc ""
					location "body"
					type "Long"
					optional true
					since "3.14.0"
				}
			}
		}

		response {
			clz APISetVolumeQosEvent.class
		}
	}
}