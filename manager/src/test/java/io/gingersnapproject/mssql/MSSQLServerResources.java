package io.gingersnapproject.mssql;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import java.util.Map;

import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.DATABASE;

public class MSSQLServerResources implements DatabaseResourcesLifecyleManager.Database {
    private static final String IMAGE = "mcr.microsoft.com/mssql/server:2019-latest";

    private MSSQLServerContainer<?> db;

    @Override
    public void start() {
        db = new MSSQLServerContainer<>(IMAGE)
                .acceptLicense()
                .withPassword("Password!42")
                .withExposedPorts(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        db.start();


        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(db, ""), "mssql/mssql-setup.sql");
        db.withUrlParam("databaseName", "gingersnap");
        ScriptUtils.runInitScript(new JdbcDatabaseDelegate(db, ""), "populate.sql");
    }

    @Override
    public void initProperties(Map<String, String> props) {
        props.put("quarkus.datasource.username", db.getUsername());
        props.put("quarkus.datasource.password", db.getPassword());
        props.put("quarkus.datasource.reactive.url", String.format("sqlserver://%s:%d/gingersnap", db.getHost(), db.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT)));
        DatabaseResourcesLifecyleManager.loadProperties("mssql/mssql-test.properties", props);
    }

    @Override
    public void stop() {
        if (db != null)
            db.stop();
    }
}
