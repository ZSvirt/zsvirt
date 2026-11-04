package org.zstack.header.vo;

/**
 * Marker for resource VO types usable in definition-layer annotations
 * (e.g. {@code BindResourceConfig}). Concrete entities implement this via {@code ResourceVO}.
 */
public interface ResourceVODefinition {
    /**
     * NopeResource is for ephemeral system tag. It can not be persisted in database.
     */
    final class NopeResource implements ResourceVODefinition {
        private NopeResource() {}
    }
}
