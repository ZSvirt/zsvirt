package org.zstack.test.integration.identity

import org.zstack.core.db.Q
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.QuotaVO
import org.zstack.header.identity.role.*
import org.zstack.testlib.AllowedDBRemaining

class IdentityDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                return vos.findAll { it.resourceType != QuotaVO.class.simpleName }
            }
        }

        table {
            tableVOClass = RoleVO.class
            checker = { List<RoleVO> vos ->
                return vos.findAll { it.type != RoleType.Predefined }
            }
        }

        table {
            tableVOClass = RolePolicyVO.class
            checker = { List<RolePolicyVO> vos ->
                List<String> roleUuids = vos.collect { it.roleUuid }

                return Q.New(RoleVO.class).notEq(RoleVO_.type, RoleType.Predefined)
                        .in(RoleVO_.uuid, roleUuids).list()
            }
        }

        table {
            tableVOClass = AccountResourceRefVO.class

            checker = { List<AccountResourceRefVO> vos ->
                def roles = Q.New(RoleVO.class).eq(RoleVO_.type, RoleType.Predefined).list() as List<RoleVO>
                List<String> systemRoleUuids = roles.collect { it.uuid }
                return vos.findAll { !systemRoleUuids.contains(it.resourceUuid) }
            }
        }
    }
}
