package in.xnnyygn.vertx.wiki.database;

import in.xnnyygn.vertx.wiki.SQLConnectionUtils;
import io.reactivex.MaybeObserver;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLClientHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WikiDatabaseServiceImpl implements WikiDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(WikiDatabaseServiceImpl.class);

    private final JDBCClient dbClient;
    private final Map<SqlQuery, String> sqlQueries;

    public WikiDatabaseServiceImpl(JDBCClient dbClient, Map<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
        this.dbClient = dbClient;
        this.sqlQueries = sqlQueries;

        if (readyHandler != null) {
            rxInitialize().subscribe(SingleHelper.toObserver(readyHandler));
        }

//    SQLClientHelper.usingConnectionSingle(dbClient, conn ->
//      conn.rxExecute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE)).andThen(Single.just(this))
//    ).subscribe(SingleHelper.toObserver(readyHandler));
    }

    @Override
    public Single<WikiDatabaseService> rxInitialize() {
        return dbClient.rxGetConnection()
                .flatMap(conn ->
                        conn.rxExecute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE))
                                .compose(SQLConnectionUtils.closeCompletable(conn))
                                .andThen(Single.just(this))
                );
    }

    //  private static <T> SingleTransformer<T, T> closeSingle(SQLConnection conn) {
//    return upstream -> upstream
//      .onErrorResumeNext(e -> conn.rxClose().andThen(Single.error(e)))
//      .flatMap(x -> conn.rxClose().andThen(Single.just(x)));
//  }

    @Override
    public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
        dbClient.rxQuery(sqlQueries.get(SqlQuery.ALL_PAGES))
                .map(rs ->
                        rs.getResults()
                                .stream()
                                .map(json -> json.getString(0))
                                .sorted()
                                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll))
                .subscribe(SingleHelper.toObserver(resultHandler));
        return this;
    }

    @Override
    public WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
        dbClient.rxQuery(sqlQueries.get(SqlQuery.ALL_PAGES_DATA))
                .map(ResultSet::getRows)
                .subscribe(SingleHelper.toObserver(resultHandler));
        return this;
    }

    @Override
    public WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {
        dbClient.rxQuerySingleWithParams(sqlQueries.get(SqlQuery.GET_PAGE), new JsonArray().add(name))
                .map(row ->
                        new JsonObject()
                                .put("found", true)
                                .put("id", row.getInteger(0))
                                .put("content", row.getString(1))
                )
                .subscribe(maybeObserver(resultHandler));
        return this;
    }

    @Override
    public WikiDatabaseService fetchPageById(int id, Handler<AsyncResult<JsonObject>> resultHandler) {
        dbClient.rxQuerySingleWithParams(sqlQueries.get(SqlQuery.GET_PAGE_BY_ID), new JsonArray().add(id))
                .map(row -> new JsonObject()
                        .put("name", row.getString(0))
                        .put("content", row.getString(1)))
                .subscribe(maybeObserver(resultHandler));
        return this;
    }

    private static MaybeObserver<JsonObject> maybeJsonFoundObserver(Handler<AsyncResult<JsonObject>> resultHandler) {
        return new MaybeObserver<JsonObject>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(JsonObject json) {
                json.put("found", true);
                resultHandler.handle(Future.succeededFuture(json));
            }

            @Override
            public void onError(Throwable e) {
                logger.error("failed", e);
                resultHandler.handle(Future.failedFuture(e));
            }

            @Override
            public void onComplete() {
                JsonObject json = new JsonObject()
                        .put("found", false);
                resultHandler.handle(Future.succeededFuture(json));
            }
        };
    }

    private static <T> MaybeObserver<T> maybeObserver(Handler<AsyncResult<T>> resultHandler) {
        return new MaybeObserver<T>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(T t) {
                resultHandler.handle(Future.succeededFuture(t));
            }

            @Override
            public void onError(Throwable e) {
                logger.error("failed", e);
                resultHandler.handle(Future.failedFuture(e));
            }

            @Override
            public void onComplete() {
                resultHandler.handle(Future.succeededFuture());
            }
        };
    }

    @Override
    public WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler) {
        logger.info("save page title {} markdown {}", title, markdown);
        // https://vertx.io/blog/simplified-database-transaction-management-with-the-vert-x-rxjava-api/
//        SQLClientHelper.inTransactionSingle(dbClient, conn -> {
//           return Single.just(true);
//        }).map(x -> new JsonObject().put("foo", x)).subscribe();
        dbClient.rxUpdateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), new JsonArray().add(title).add(markdown))
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(resultHandler));
        return this;
    }

    @Override
    public WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler) {
        logger.info("update page {} markdown {}", id, markdown);
        dbClient.rxUpdateWithParams(sqlQueries.get(SqlQuery.SAVE_PAGE), new JsonArray().add(markdown).add(id))
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(resultHandler));
        return this;
    }

    @Override
    public WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {
        dbClient.rxUpdateWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), new JsonArray().add(id))
                .ignoreElement()
                .subscribe(CompletableHelper.toObserver(resultHandler));
        return this;
    }
}
