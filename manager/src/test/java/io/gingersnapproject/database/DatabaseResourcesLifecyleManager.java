package io.gingersnapproject.database;

import io.gingersnapproject.mysql.MySQLResources;
import io.gingersnapproject.postgres.PostgresResources;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DatabaseResourcesLifecyleManager implements QuarkusTestResourceLifecycleManager {

    public static final String DATABASE = "gingersnap";

    private Database db;

    @Override
    public void setContext(Context context) {
        var dbKind = System.getProperty("quarkus.datasource.db-kind");
        switch (dbKind) {
            case "postgresql" -> db = new PostgresResources();
            default -> db = new MySQLResources();
        }
    }

    @Override
    public Map<String, String> start() {
        db.start();
        Map<String, String> properties = new HashMap<>();
        db.initProperties(properties);
        return properties;
    }

    @Override
    public void stop() {
        db.stop();
    }

    public static void loadProperties(String resource, Map<String, String> properties) {
        try (InputStream is = DatabaseResourcesLifecyleManager.class.getClassLoader().getResourceAsStream(resource)) {
            Properties p = new Properties();
            p.load(is);
            p.forEach((k, v) -> properties.put((String) k, (String) v));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Database {

        void initProperties(Map<String, String> props);

        void start();

        void stop();
    }
}
