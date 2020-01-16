package in.xnnyygn.vertx.wiki.database;

import io.reactivex.Single;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.jdbc.JDBCClient;

import java.util.List;
import java.util.Map;

@ProxyGen
@VertxGen
public interface WikiDatabaseService {

  @GenIgnore
  default Single<WikiDatabaseService> rxInitialize() {
    return Single.never();
  }

  @Fluent
  WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler);

  @Fluent
  WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler);

  @Fluent
  WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler);

  @GenIgnore
  static WikiDatabaseService create(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    return new WikiDatabaseServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  @GenIgnore
  static Single<WikiDatabaseService> rxCreate(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries) {
    WikiDatabaseService service = new WikiDatabaseServiceImpl(dbClient, sqlQueries, null);
    return service.rxInitialize();
  }

  @GenIgnore
  static in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService createProxy(Vertx vertx, String address) {
    return new in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService(new WikiDatabaseServiceVertxEBProxy(vertx, address));
  }
}
