package org.zstack.header.rest;

import org.zstack.header.identity.SessionInventory;

/**
 * Created with IntelliJ IDEA.
 * User: shixin
 * Time: 1:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RestAuthenticationBackend {
    RestAuthenticationType getAuthenticationType();

    SessionInventory doAuth(RestAuthenticationParams prams) throws RestException;
}
