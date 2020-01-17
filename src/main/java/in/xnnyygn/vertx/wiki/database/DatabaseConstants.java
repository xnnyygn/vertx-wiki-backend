package in.xnnyygn.vertx.wiki.database;

public class DatabaseConstants {
    public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
    public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
    public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";
    public static final String CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE = "wikidb.sqlqueries.resources.file";
    public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

    public static final String DEFAULT_WIKIDB_JDBC_URL = "jdbc:hsqldb:file:db/wiki";
    public static final String DEFAULT_WIKIDB_JDBC_DRIVER_CLASS = "org.hsqldb.jdbcDriver";
    public static final int DEFAULT_JDBC_MAX_POOL_SIZE = 30;
}
