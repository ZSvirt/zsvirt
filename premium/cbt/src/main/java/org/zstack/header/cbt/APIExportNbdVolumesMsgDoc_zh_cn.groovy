package org.zstack.header.cbt

import org.zstack.header.cbt.APIExportNbdVolumesEvent

doc {
	title "ExportNbdVolumes"

	category "cbt"

	desc """暴露云盘为NBD设备"""

	rest {
		request {
			url "POST /v1/cbt-task/exportvolume"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExportNbdVolumesMsg.class

			desc """"""

			params {

				column {
					name "volumeUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "4.10.10"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "4.10.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "force"
					enclosedIn "params"
					desc ""
					location "body"
					type "boolean"
					optional true
					since "4.10.10"
				}
			}
		}

		response {
			clz APIExportNbdVolumesEvent.class
		}
	}
}