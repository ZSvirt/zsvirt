package org.zstack.core.privatekey;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.privatekey.PrivateKeyConstant;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ed25519KeyPairs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class Ed25519KeyClient {
    public String ensureCommonsKeyPair() throws IOException {
        File folder = new File(PrivateKeyConstant.PRIVATE_KEY_FOLDER);
        File privateKey = new File(folder, PrivateKeyConstant.COMMONS_PRIVATE_KEY_PATH);
        File publicKey = new File(folder, PrivateKeyConstant.COMMONS_PUBLIC_KEY_PATH);
        File sshDir = new File(PathUtil.getZStackHomeFolder(), ".ssh");
        File privateLink = new File(sshDir, PrivateKeyConstant.DEFAULT_PRIVATE_KEY_NAME);
        File publicLink = new File(sshDir, PrivateKeyConstant.DEFAULT_PUBLIC_KEY_NAME);

        assertDefaultIdentityAvailable(privateLink, privateKey);
        assertDefaultIdentityAvailable(publicLink, publicKey);

        String publicKeyText = Ed25519KeyPairs.ensure(privateKey, publicKey);
        linkDefaultIdentity(privateLink, privateKey);
        linkDefaultIdentity(publicLink, publicKey);
        return publicKeyText;
    }

    private static void assertDefaultIdentityAvailable(File link, File commonsKey) throws IOException {
        Path linkPath = link.toPath();
        if (!Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(linkPath) && sameFile(Files.readSymbolicLink(linkPath), linkPath.getParent(), commonsKey)) {
            return;
        }
        throw new IOException(String.format(
                "default SSH identity already exists at %s", link.getAbsolutePath()));
    }

    private static void linkDefaultIdentity(File link, File commonsKey) throws IOException {
        Path linkPath = link.toPath();
        if (Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        File sshDir = link.getParentFile();
        if (sshDir != null && !sshDir.exists() && !sshDir.mkdirs()) {
            throw new IOException(String.format("failed to create directory %s", sshDir.getAbsolutePath()));
        }
        chmod(sshDir, "rwx------");
        Files.createSymbolicLink(linkPath, commonsKey.getAbsoluteFile().toPath());
    }

    private static boolean sameFile(Path symlinkTarget, Path linkParent, File commonsKey) {
        Path expected = commonsKey.getAbsoluteFile().toPath().normalize();
        Path actual = symlinkTarget.isAbsolute() ? symlinkTarget.normalize() : linkParent.resolve(symlinkTarget).normalize();
        return expected.equals(actual);
    }

    private static void chmod(File file, String perms) {
        if (file == null) {
            return;
        }
        try {
            PathUtil.setFilePosixPermissions(file, perms);
        } catch (UnsupportedOperationException ignored) {
            // POSIX permissions are not available on this filesystem.
        }
    }
}
