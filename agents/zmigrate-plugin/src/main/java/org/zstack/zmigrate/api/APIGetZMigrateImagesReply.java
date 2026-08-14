package org.zstack.zmigrate.api;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APIGetZMigrateImagesReply extends APIReply {
    private Map<String, ImageInventory> images = new HashMap<>();

    public Map<String, ImageInventory> getImages() {
        return images;
    }

    public void setImages(Map<String, ImageInventory> images) {
        this.images = images;
    }

    public static APIGetZMigrateImagesReply __example__() {
        APIGetZMigrateImagesReply reply = new APIGetZMigrateImagesReply();
        Map<String, ImageInventory> images = new HashMap<>();
        ImageInventory image = new ImageInventory();
        image.setUuid(uuid(ImageVO.class));
        image.setName("test-image");
        image.setDescription("test image for zmigrate");
        images.put("ZMigrateGatewayImage", image);
        reply.setImages(images);
        return reply;
    }
}