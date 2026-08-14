package org.zstack.header.console;


import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleInventory {
    private String scheme;
    private String targetScheme;
    private String hostname;
    private int port;
    private String token;
    private String version;
    private Timestamp expiredDate;

    public static ConsoleInventory valueOf(ConsoleProxyVO vo) {
        ConsoleInventory inv = new ConsoleInventory();
        inv.setToken(vo.getToken());
        inv.setHostname(vo.getProxyHostname());
        inv.setPort(vo.getProxyPort());
        inv.setScheme(vo.getScheme());
        inv.setTargetScheme(vo.getTargetSchema());
        inv.setVersion(vo.getVersion());
        inv.setExpiredDate(vo.getExpiredDate());
        return inv;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getTargetScheme() {
        return targetScheme;
    }

    public void setTargetScheme(String targetScheme) {
        this.targetScheme = targetScheme;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Timestamp getExpiredDate() {
        return expiredDate;
    }
    public void setExpiredDate(Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }
}
