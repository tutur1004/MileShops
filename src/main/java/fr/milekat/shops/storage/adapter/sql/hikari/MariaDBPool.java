package fr.milekat.shops.storage.adapter.sql.hikari;

import com.zaxxer.hikari.HikariConfig;
import fr.milekat.shops.Main;
import org.jetbrains.annotations.NotNull;
import org.mariadb.jdbc.MariaDbDataSource;

import java.sql.SQLException;

public class MariaDBPool extends HikariPool {
    @Override
    public String getImplementationName() {
        return "MariaDB";
    }

    @Override
    protected void configureDatabase(@NotNull HikariConfig config, String address, String port,
                                     String databaseName, String username, String password) {
        try {
            MariaDbDataSource mariaDbDataSource = new MariaDbDataSource();
            mariaDbDataSource.setUrl("jdbc:mariadb://" + address + ":" + port + "/" + databaseName);
            mariaDbDataSource.setUser(username);
            mariaDbDataSource.setPassword(password);
            config.setDataSource(mariaDbDataSource);
        } catch (SQLException exception) {
            Main.warning("MariaDBPool error");
            exception.printStackTrace();
        }
    }
}
