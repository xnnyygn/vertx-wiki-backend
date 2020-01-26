package in.xnnyygn.vertx.wiki.database;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class WikiDatabaseVerticle extends AbstractVerticle {

    private Single<Map<SqlQuery, String>> rxLoadSqlQueries() {
        return vertx.fileSystem()
                .rxReadFile(config().getString(DatabaseConstants.CONFIG_WIKIDB_SQL_QUERIES_RESOURCE_FILE, "db-queries.properties"))
                .flatMap(buffer -> {
                    Properties props = new Properties();
                    try {
                        props.load(new BufferInputStream(buffer));
                        return Single.just(props);
                    } catch (IOException | IllegalArgumentException e) {
                        return Single.error(e);
                    }
                }).map(props -> {
                    Map<SqlQuery, String> sqlQueries = new HashMap<>();
                    sqlQueries.put(SqlQuery.CREATE_PAGES_TABLE, props.getProperty("create-pages-table"));
                    sqlQueries.put(SqlQuery.ALL_PAGES, props.getProperty("all-pages"));
                    sqlQueries.put(SqlQuery.ALL_PAGES_DATA, props.getProperty("all-pages-data"));
                    sqlQueries.put(SqlQuery.GET_PAGE, props.getProperty("get-page"));
                    sqlQueries.put(SqlQuery.GET_PAGE_BY_ID, props.getProperty("get-page-by-id"));
                    sqlQueries.put(SqlQuery.CREATE_PAGE, props.getProperty("create-page"));
                    sqlQueries.put(SqlQuery.SAVE_PAGE, props.getProperty("save-page"));
                    sqlQueries.put(SqlQuery.DELETE_PAGE, props.getProperty("delete-page"));
                    return sqlQueries;
                });
    }

    public static class BufferInputStream extends InputStream {
        private final Buffer buffer;
        private int position = 0;

        public BufferInputStream(Buffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int read() {
            if (position >= buffer.length()) {
                return -1;
            }
            return buffer.getByte(position++);
        }
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JDBCClient dbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getString(DatabaseConstants.CONFIG_WIKIDB_JDBC_URL, DatabaseConstants.DEFAULT_WIKIDB_JDBC_URL))
                .put("driver_class", config().getString(DatabaseConstants.CONFIG_WIKIDB_JDBC_DRIVER_CLASS, DatabaseConstants.DEFAULT_WIKIDB_JDBC_DRIVER_CLASS))
                .put("max_pool_size", config().getInteger(DatabaseConstants.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, DatabaseConstants.DEFAULT_JDBC_MAX_POOL_SIZE)));

        rxLoadSqlQueries()
                .flatMap(sqlQueries -> WikiDatabaseService.rxCreate(dbClient, sqlQueries))
                .subscribe(new SingleObserver<WikiDatabaseService>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onSuccess(WikiDatabaseService wikiDatabaseService) {
                        ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
                        binder.setAddress(DatabaseConstants.CONFIG_WIKIDB_QUEUE);
                        binder.register(WikiDatabaseService.class, wikiDatabaseService);
                        startPromise.complete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        startPromise.fail(e);
                    }
                });

//    WikiDatabaseService.create(dbClient, sqlQueries, ready -> {
//      if (ready.failed()) {
//        startPromise.fail(ready.cause());
//        return;
//      }
//      ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
//      binder.setAddress(CONFIG_WIKIDB_QUEUE);
//      binder.register(WikiDatabaseService.class, ready.result());
//      startPromise.complete();
//    });
    }
}
