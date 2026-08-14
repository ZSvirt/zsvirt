package org.zstack.header.vmscheduling

import org.zstack.header.affinitygroup.APICreateAffinityGroupEvent

doc {
	title "CreateVmSchedulingRule"

	category "vmSchedulingRule"

	desc """创建vm调度规则"""

	rest {
		request {
			url "POST /v1/vmsSchedulingRule"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmSchedulingRuleMsg.class

			desc """"""

			params {

				column {
					name "rule"
					enclosedIn "params"
					desc "规则"
					location "body"
					type "String"
					optional false
					since "3.16.0"
					values ("AFFINITY","ANTIAFFINITY")
				}
				column {
					name "mode"
					enclosedIn "params"
					desc "执行力度"
					location "body"
					type "String"
					optional false
					since "3.16.0"
					values ("SOFT","HARD")
				}
				column {
					name "vmGroupUuid"
					enclosedIn "params"
					desc "vm调度组uuid"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "hostGroupUuid"
					enclosedIn "params"
					desc "物理机调度组uuid"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "policy"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.16.0"
					values ("antiSoft","antiHard")
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.16.0"
					values ("host")
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "subType"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APICreateAffinityGroupEvent.class
		}
	}
}