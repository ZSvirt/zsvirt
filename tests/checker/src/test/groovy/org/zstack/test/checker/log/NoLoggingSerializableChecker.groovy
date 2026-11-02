package org.zstack.test.checker.log

import org.junit.Test
import org.zstack.header.log.NoLogging
import org.zstack.header.message.Message
import org.zstack.utils.BeanUtils

import java.lang.reflect.Field

/**
 * Scope: every {@code org.zstack} class on this module's classpath (zsvirt-full,
 * premium included, zsvirt-ee not included) that declares a field annotated with
 * {@link NoLogging}. The declaring class must implement {@link Serializable} or
 * {@link Message}. Both {@code Mask} and {@code Auto} are checked. This is the
 * condition {@code LogSafeGson} uses before it records the annotation: only
 * subtypes of those two types are scanned, and the class package must be
 * {@code org.zstack}. A declaring class outside that set is logged in plaintext.
 * <p>
 * The subject is the field's declaring class, not the Java enclosing class.
 * {@code CephPrimaryStorageBase.KVMHostLuksCloneCmd} implements {@code Serializable};
 * {@code CephPrimaryStorageBase} does not, and is not a failure.
 * <p>
 * Not covered:
 * <ul>
 *   <li>Nested masking targets. {@code behavior = Auto} walks into the runtime
 *       value, and that value often does not declare {@code @NoLogging}.
 *       {@code CanonicalEvent.content} is {@code Object}; payloads such as
 *       {@code PrimaryStorageStatusChangedData} are invisible here. A missing
 *       {@code Serializable} on those payloads still leaks nested fields.</li>
 *   <li>{@code classNameField}. {@code KVMHostHttpCallMsg.command} is a
 *       {@code String}; the command class is {@code Class.forName(commandClassName)}
 *       at runtime. Only the message class itself is checked.</li>
 *   <li>Raw collections. {@code ZQLQueryReturn.inventories} is a raw {@code List};
 *       element types are erased and cannot be enumerated.</li>
 *   <li>Declaring class only. A superclass that declares {@code @NoLogging} and
 *       implements neither type fails even when a subclass implements one and
 *       {@code LogSafeGson} still masks the subclass through inherited fields.</li>
 *   <li>Interfaces are skipped, matching {@code LogSafeGson}, which does not
 *       index interface types.</li>
 * </ul>
 * Disabled in {@code JUnitCheckTestSuite}. Agent command and response classes
 * such as {@code KVMAgentCommands.VmDevicesInfoResponse} declare {@code @NoLogging}
 * and implement neither type, so enabling this fails on the current tree.
 */
class NoLoggingSerializableChecker {
    @Test
    void checkNoLoggingDeclaringClass() {
        Map<String, List<String>> violations = new TreeMap<>()
        BeanUtils.reflections.getFieldsAnnotatedWith(NoLogging.class).each { Field field ->
            Class<?> clz = field.declaringClass
            if (clz.isInterface()) {
                return
            }
            Package pkg = clz.package
            if (pkg == null || !pkg.name.startsWith("org.zstack")) {
                return
            }
            if (Serializable.isAssignableFrom(clz) || Message.isAssignableFrom(clz)) {
                return
            }
            violations.computeIfAbsent(clz.name) { new ArrayList<String>() }.add(field.name)
        }

        assert violations.isEmpty(): "classes declaring @NoLogging must implement Serializable or Message: " +
                violations.collect { name, fields -> "${name} [${fields.join(', ')}]" }.join("; ")
    }
}
