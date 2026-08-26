package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainScene;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = AllocatorDomainSceneVO.class)
public class AllocatorDomainSceneInventory implements Serializable {
    private Long id;
    private String domainUuid;
    private String scene;
    private Timestamp createDate;

    public static AllocatorDomainSceneInventory valueOf(AllocatorDomainSceneVO vo) {
        AllocatorDomainSceneInventory inv = new AllocatorDomainSceneInventory();
        inv.setId(vo.getId());
        inv.setDomainUuid(vo.getDomainUuid());
        inv.setScene(vo.getScene() == null ? null : vo.getScene().toString());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<AllocatorDomainSceneInventory> valueOf(Collection<AllocatorDomainSceneVO> vos) {
        List<AllocatorDomainSceneInventory> invs = new ArrayList<>();
        if (vos == null) {
            return invs;
        }
        for (AllocatorDomainSceneVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public static AllocatorDomainSceneInventory __example__() {
        AllocatorDomainSceneInventory inv = new AllocatorDomainSceneInventory();
        inv.setId(1L);
        inv.setDomainUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        inv.setScene(AllocatorDomainScene.Start.toString());
        return inv;
    }
}
