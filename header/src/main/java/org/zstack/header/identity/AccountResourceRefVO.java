package org.zstack.header.identity;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = AccountVO.class, myField = "accountUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ResourceVO.class, myField = "resourceUuid", targetField = "uuid")
        }
)
public class AccountResourceRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String accountUuid;

    @Column
    @Index
    private String resourceUuid;

    // may be deprecated later, should not rely on it
    @Column
    @Index
    private String resourceType;

    @Column
    private String accountPermissionFrom;

    @Column
    private String resourcePermissionFrom;

    @Column
    @Enumerated(EnumType.STRING)
    private AccessLevel type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAccountPermissionFrom() {
        return accountPermissionFrom;
    }

    public void setAccountPermissionFrom(String accountPermissionFrom) {
        this.accountPermissionFrom = accountPermissionFrom;
    }

    public String getResourcePermissionFrom() {
        return resourcePermissionFrom;
    }

    public void setResourcePermissionFrom(String resourcePermissionFrom) {
        this.resourcePermissionFrom = resourcePermissionFrom;
    }

    public AccessLevel getType() {
        return type;
    }

    public void setType(AccessLevel type) {
        this.type = type;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public static AccountResourceRefVO forOwner(String accountUuid, String resourceUuid, Class<?> baseType) {
        AccountResourceRefVO ref = new AccountResourceRefVO();
        ref.setAccountUuid(accountUuid);
        ref.setResourceType(baseType.getSimpleName());
        ref.setResourceUuid(resourceUuid);
        ref.setType(AccessLevel.Own);
        return ref;
    }
}
