package org.zstack.testlib.identity.ldap

import org.zstack.ldap.entity.LdapEntryAttributeInventory
import org.zstack.ldap.entity.LdapEntryInventory

class LdapEntryInventoryBuilder {
    private LdapEntryInventory inventory

    {
        inventory = new LdapEntryInventory()
        inventory.attributes = []
        inventory.enable = true
    }

    LdapEntryInventoryBuilder enable() {
        inventory.enable = true
        return this
    }

    LdapEntryInventoryBuilder disable() {
        inventory.enable = false
        return this
    }

    LdapEntryInventoryBuilder withDn(String dn) {
        inventory.dn = dn
        return this
    }

    LdapEntryInventoryBuilder withAttribute(String id, Object value) {
        def attribute = new LdapEntryAttributeInventory()
        attribute.id = id
        attribute.values = [value]
        attribute.orderMatters = false

        inventory.attributes.add(attribute)
        return this
    }

    LdapEntryInventoryBuilder withAttributeSet(String id, Object... values) {
        def attribute = new LdapEntryAttributeInventory()
        attribute.id = id
        attribute.values = Arrays.asList(values)
        attribute.orderMatters = false

        inventory.attributes.add(attribute)
        return this
    }

    LdapEntryInventoryBuilder withAttributeList(String id, Object... values) {
        def attribute = new LdapEntryAttributeInventory()
        attribute.id = id
        attribute.values = Arrays.asList(values)
        attribute.orderMatters = true

        inventory.attributes.add(attribute)
        return this
    }

    LdapEntryInventory build() {
        return inventory
    }
}
