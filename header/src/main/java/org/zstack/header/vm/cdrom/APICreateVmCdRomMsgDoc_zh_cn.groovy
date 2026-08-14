package org.zstack.header.vm.cdrom

import org.zstack.header.vm.cdrom.APICreateVmCdRomEvent

doc {
	title "CreateVmCdRom"

	category "vmInstance"

	desc """为云主机创建CDROM"""

	rest {
		request {
			url "POST /v1/vm-instances/cdroms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmCdRomMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "zsv 3.3"
				}
				column {
					name "isoUuid"
					enclosedIn "params"
					desc "ISO镜像UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 3.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.3"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 3.4.0"
				}
			}
		}

		response {
			clz APICreateVmCdRomEvent.class
		}
	}
}