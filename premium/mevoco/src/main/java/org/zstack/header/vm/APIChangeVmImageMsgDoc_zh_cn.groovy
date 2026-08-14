package org.zstack.header.vm

import org.zstack.header.vm.APIChangeVmImageEvent

doc {
	title "ChangeVmImage"

	category "mevoco"

	desc """修改云主机根云盘"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeVmImageMsg.class

			desc """修改云主机根云盘"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "changeVmImage"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "imageUuid"
					enclosedIn "changeVmImage"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "resourceUuid"
					enclosedIn "changeVmImage"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "changeVmImage"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIChangeVmImageEvent.class
		}
	}
}