package org.zstack.header.image;

/**
 */
public enum ImageErrors {
    CREATE_IMAGE_FROM_VOLUME_ERROR(1000),
    UPLOAD_IMAGE_INTERRUPTED(1001);
    private String code;

    private ImageErrors(int id) {
        code = String.format("IMG.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
