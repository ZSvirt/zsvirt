package org.zstack.imagereplicator;

import org.zstack.header.image.ImageInventory;

public interface ImageJournalGenerator {
    void generateInitialRecords(String bsUuid);
    void onUpdateImage(ImageInventory inv);
    void onAddImage(ImageInventory inv);
    void onExpungeImage(String imageUuid, String bsUuid);
}
