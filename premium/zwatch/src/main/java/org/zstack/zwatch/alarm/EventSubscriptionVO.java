package org.zstack.zwatch.alarm;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;
import org.zstack.zwatch.datatype.EmergencyLevel;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = EventSubscriptionActionVO.class, myField = "uuid", targetField = "subscriptionUuid"),
                @EntityGraph.Neighbour(type = EventSubscriptionLabelVO.class, myField = "uuid", targetField = "subscriptionUuid"),
        }
)
public class EventSubscriptionVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;
    @Column
    private String namespace;
    @Column
    private String eventName;
    @Column
    @Enumerated(EnumType.STRING)
    private EventSubscriptionState state;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscriptionUuid", insertable = false, updatable = false)
    private Set<EventSubscriptionActionVO> actions = new HashSet<>();
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscriptionUuid", insertable = false, updatable = false)
    private Set<EventSubscriptionLabelVO> labels = new HashSet<>();
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @Column
    @Enumerated(EnumType.STRING)
    private EmergencyLevel emergencyLevel;

    @Transient
    private String accountUuid;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public EventSubscriptionState getState() {
        return state;
    }

    public void setState(EventSubscriptionState state) {
        this.state = state;
    }

    public Set<EventSubscriptionActionVO> getActions() {
        return actions;
    }

    public void setActions(Set<EventSubscriptionActionVO> actions) {
        this.actions = actions;
    }

    public Set<EventSubscriptionLabelVO> getLabels() {
        return labels;
    }

    public void setLabels(Set<EventSubscriptionLabelVO> labels) {
        this.labels = labels;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}
