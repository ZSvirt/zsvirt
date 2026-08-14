package org.zstack.header.volume.block;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = BlockVolumeVO.class, collectionValueOfMethod="valueOf1",
        parent = {@Parent(inventoryClass = VolumeInventory.class, type = VolumeConstant.BLOCK_VOLUME_TYPE)})
@PythonClassInventory
public class BlockVolumeInventory extends VolumeInventory {
   private String iscsiPath;
   
   private String vendor;

   public String getVendor() {
      return vendor;
   }

   public void setVendor(String vendor) {
      this.vendor = vendor;
   }

   public String getIscsiPath() {
        return iscsiPath;
   }

   public void setIscsiPath(String iscsiPath) {
        this.iscsiPath = iscsiPath;
   }
   
   public static BlockVolumeInventory valueOf(BlockVolumeVO vo) {
      return new BlockVolumeInventory(vo);
   }

   public BlockVolumeInventory() {
   }

   public BlockVolumeInventory(BlockVolumeVO other) {
      super(VolumeInventory.valueOf(other));
      this.vendor = other.getVendor();
      this.iscsiPath = other.getIscsiPath();
   }

   public static List<BlockVolumeInventory> valueOf1(Collection<BlockVolumeVO> vos) {
      List<BlockVolumeInventory> invs = new ArrayList<BlockVolumeInventory>(vos.size());
      for (BlockVolumeVO vo : vos) {
         invs.add(new BlockVolumeInventory(vo));
      }
      return invs;
   }
}
