package org.zstack.header.privatekey;

public class PrivateKeyConstant {
    /**
     * ED25519 keys is in "/var/lib/zstack/pk"
     *
     * /var/lib/zstack/pk/
     * └── commons/
     *     ├── ed25519
     *     └── ed25519.pub
     */
    public static final String PRIVATE_KEY_FOLDER = "/var/lib/zstack/pk";
    public static final String COMMONS_PRIVATE_KEY_PATH = "commons/ed25519";
    public static final String COMMONS_PUBLIC_KEY_PATH = "commons/ed25519.pub";

    public static final String DEFAULT_PRIVATE_KEY_NAME = "id_ed25519";
    public static final String DEFAULT_PUBLIC_KEY_NAME = "id_ed25519.pub";

    public static final String ED25519_SERVICE_ID = "ed25519";
}
