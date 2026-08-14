package org.zstack.core.db;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

public class JpaUnitPostProcessor implements PersistenceUnitPostProcessor {
    private String dbUser;
    private String dbPassword;
    private String dbHost;
    private String dbName;
    private BasicDataSource dataSource = null;
    
    @Override
    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo unit) {
        /*
        unit.addProperty("hibernate.connection.username", getDbUser());
        unit.addProperty("hibernate.connection.password", getDbPassword());
        String jdbcUrl = String.format("jdbc:mysql://%s/%s", getDbHost(), getDbName());
        unit.addProperty("hibernate.connection.url", jdbcUrl);
        unit.addProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        unit.addProperty("hibernate.dialect", "org.hibernate.dialect.MySQLInnoDBDialect");
        unit.addProperty("org.jboss.logging.provider", "log4j2");
        */
    }

    void init() {
    	dataSource = new BasicDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		String jdbcUrl = String.format("jdbc:mysql://%s/%s", getDbHost(), getDbName());
		dataSource.setUrl(jdbcUrl);
		dataSource.setUsername(getDbUser());
		dataSource.setPassword(getDbPassword());
    }
    
    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    public BasicDataSource getDataSource() {
    	return dataSource;
    }
}
