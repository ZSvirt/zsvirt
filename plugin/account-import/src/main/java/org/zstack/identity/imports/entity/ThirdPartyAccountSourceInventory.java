package org.zstack.identity.imports.entity;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/12/05
 */
@Inventory(mappingVOClass = ThirdPartyAccountSourceVO.class)
@PythonClassInventory
public class ThirdPartyAccountSourceInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private String createAccountStrategy;
    private List<String> updateAccountStrategies;
    private String deleteAccountStrategy;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ThirdPartyAccountSourceInventory valueOf(ThirdPartyAccountSourceVO vo) {
        ThirdPartyAccountSourceInventory inv = new ThirdPartyAccountSourceInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getResourceName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType());
        inv.setCreateAccountStrategy(Objects.toString(vo.getCreateAccountStrategy()));
        inv.setDeleteAccountStrategy(Objects.toString(vo.getDeleteAccountStrategy()));
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        List<String> updateStrategies = new ArrayList<>();
        if (!StringUtils.isEmpty(vo.getUpdateAccountStrategies())) {
            List<SyncUpdateAccountStrategy> strategies =
                    SyncUpdateAccountStrategy.valueOfStrategies(vo.getUpdateAccountStrategies());
            updateStrategies.addAll(CollectionUtils.transform(strategies, Enum::name));
        }
        inv.setUpdateAccountStrategies(updateStrategies);

        return inv;
    }

    public static List<ThirdPartyAccountSourceInventory> valueOf(Collection<ThirdPartyAccountSourceVO> vos) {
        return CollectionUtils.transform(vos, ThirdPartyAccountSourceInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCreateAccountStrategy() {
        return createAccountStrategy;
    }

    public void setCreateAccountStrategy(String createAccountStrategy) {
        this.createAccountStrategy = createAccountStrategy;
    }

    public List<String> getUpdateAccountStrategies() {
        return updateAccountStrategies;
    }

    public void setUpdateAccountStrategies(List<String> updateAccountStrategies) {
        this.updateAccountStrategies = updateAccountStrategies;
    }

    public String getDeleteAccountStrategy() {
        return deleteAccountStrategy;
    }

    public void setDeleteAccountStrategy(String deleteAccountStrategy) {
        this.deleteAccountStrategy = deleteAccountStrategy;
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

    public static ThirdPartyAccountSourceInventory __example__() {
        ThirdPartyAccountSourceInventory inventory = new ThirdPartyAccountSourceInventory();
        inventory.setUuid(DocUtils.createFixedUuid(ThirdPartyAccountSourceVO.class));
        inventory.setName("Test-ldap");
        inventory.setDescription("some descriptions");
        inventory.setType("OpenLdap");
        inventory.setCreateAccountStrategy(SyncCreatedAccountStrategy.CreateDisabledAccount.toString());
        inventory.setUpdateAccountStrategies(list(SyncUpdateAccountStrategy.AccountStateKeepSameWithSource.toString()));
        inventory.setDeleteAccountStrategy(SyncDeletedAccountStrategy.StaleAccount.toString());
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        return inventory;
    }
}
