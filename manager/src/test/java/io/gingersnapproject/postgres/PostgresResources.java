package io.gingersnapproject.postgres;

import io.gingersnapproject.database.DatabaseResourcesLifecyleManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

import static io.gingersnapproject.database.DatabaseResourcesLifecyleManager.DATABASE;

public class PostgresResources implements DatabaseResourcesLifecyleManager.Database {

    private static final String IMAGE = "postgres:latest";

    private PostgreSQLContainer<?> db;

    @Override
    public void start() {
        db = new PostgreSQLContainer<>(IMAGE)
                .withDatabaseName(DATABASE)
                .withUsername("gingersnap_user")
                .withPassword("password")
                .withExposedPorts(PostgreSQLContainer.POSTGRESQL_PORT)
                .withTmpFs(Map.of("/var/lib/postgresql/data", "rw"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("postgres/postgres-setup.sql"), "/docker-entrypoint-initdb.d/postgres-setup.sql")
                .withCopyFileToContainer(MountableFile.forClasspathResource("populate.sql"), "/docker-entrypoint-initdb.d/z_populate.sql");
        db.start();
    }

    @Override
    public void initProperties(Map<String, String> props) {
        props.put("quarkus.datasource.username", db.getUsername());
        props.put("quarkus.datasource.password", db.getPassword());
        props.put("quarkus.datasource.reactive.url", String.format("postgresql://%s:%d/%s", db.getHost(), db.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), db.getDatabaseName()));
        DatabaseResourcesLifecyleManager.loadProperties("postgres/postgres-test.properties", props);
    }


    @Override
    public void stop() {
        if (db != null)
            db.stop();
    }
}
