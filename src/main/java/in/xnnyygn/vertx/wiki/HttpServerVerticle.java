package in.xnnyygn.vertx.wiki;

import com.github.rjeschke.txtmark.Processor;
import in.xnnyygn.vertx.wiki.database.DatabaseConstants;
import in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
    public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

    private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
            "\n" +
            "Feel-free to write in Markdown!\n";

    private WikiDatabaseService dbService;
    private FreeMarkerTemplateEngine templateEngine;
    private WebClient webClient;

    @Override
    public void start(Promise<Void> startPromise) {
        String wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
        dbService = in.xnnyygn.vertx.wiki.database.WikiDatabaseService.createProxy(vertx.getDelegate(), wikiDbQueue);

        JDBCClient authDbClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getString(DatabaseConstants.CONFIG_WIKIDB_JDBC_URL, DatabaseConstants.DEFAULT_WIKIDB_JDBC_URL))
                .put("driver_class", config().getString(DatabaseConstants.CONFIG_WIKIDB_JDBC_DRIVER_CLASS, DatabaseConstants.DEFAULT_WIKIDB_JDBC_DRIVER_CLASS))
                .put("max_pool_size", config().getInteger(DatabaseConstants.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, DatabaseConstants.DEFAULT_JDBC_MAX_POOL_SIZE)));
        JDBCAuth auth = JDBCAuth.create(vertx, authDbClient);

//        JksOptions jksOptions = new JksOptions()
//                .setPath("server-keystore.jks")
//                .setPassword("secret");
//        HttpServerOptions serverOptions = new HttpServerOptions()
//                .setSsl(true)
//                .setKeyStoreOptions(jksOptions);
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(UserSessionHandler.create(auth));

        AuthHandler authHandler = RedirectAuthHandler.create(auth, "/login");
        router.route("/").handler(authHandler);
        router.route("/wiki/*").handler(authHandler);
        router.route("/action/*").handler(authHandler);

        router.get("/login").handler(this::loginHandler);
        router.post("/login-auth").handler(FormLoginHandler.create(auth));
        router.get("/logout").handler(this::logoutHandler);

        router.get("/").handler(this::indexHandler);
        router.get("/wiki/:page").handler(this::pageRenderingHandler);
        router.get("/action/backup").handler(this::backupHandler);
        router.post().handler(BodyHandler.create());
        router.post("/action/create").handler(this::pageCreateHandler);
        router.post("/action/save").handler(this::pageSaveHandler);
        router.post("/action/delete").handler(this::pageDeleteHandler);

        Router apiRouter = Router.router(vertx);
        apiRouter.get("/pages").handler(this::apiRoot);
        apiRouter.get("/pages/:id").handler(this::apiGetPage);
        apiRouter.post().handler(BodyHandler.create());
        apiRouter.post("/pages").handler(this::apiCreateHandler);
        apiRouter.put().handler(BodyHandler.create());
        apiRouter.put("/pages/:id").handler(this::apiUpdateHandler);
        apiRouter.delete("/pages/:id").handler(this::apiDeletePage);
        router.mountSubRouter("/api", apiRouter);

        templateEngine = FreeMarkerTemplateEngine.create(vertx);

        WebClientOptions webClientOptions = new WebClientOptions()
                .setSsl(true)
                .setUserAgent("vertx-v3");
        webClient = WebClient.create(vertx, webClientOptions);

        int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);

        server.requestHandler(router)
                .listen(portNumber, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port " + portNumber);
                        startPromise.complete();
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause());
                        startPromise.fail(ar.cause());
                    }
                });
    }

    private void logoutHandler(RoutingContext context) {
        context.clearUser();
        redirect(context, "/");
    }

    private void loginHandler(RoutingContext context) {
        context.put("title", "Login");
        templateEngine.rxRender(context.data(), "templates/login.ftl")
                .subscribe(renderHtmlObserver(context));
    }

    private void apiDeletePage(RoutingContext context) {
        int id = Integer.parseInt(context.request().getParam("id"));
        dbService.rxDeletePage(id)
                .subscribe(apiCompletableObserver(context, 204));
    }

    private void apiUpdateHandler(RoutingContext context) {
        JsonObject page = context.getBodyAsJson();
        if (!validateJsonPage(page, "content")) {
            apiReplyBadRequest(context);
            return;
        }
        int id = Integer.parseInt(context.request().getParam("id"));
        dbService.rxSavePage(id, page.getString("content"))
                .subscribe(apiCompletableObserver(context, 200));
    }

    private void apiCreateHandler(RoutingContext context) {
        JsonObject page = context.getBodyAsJson();
        if (!validateJsonPage(page, "name", "content")) {
            apiReplyBadRequest(context);
            return;
        }
        dbService.rxCreatePage(page.getString("name"), page.getString("content"))
                .subscribe(apiCompletableObserver(context, 201));
    }

    private void apiReplyBadRequest(RoutingContext context) {
        apiReply(context, 400, new JsonObject()
                .put("success", false)
                .put("error", "bad request payload"));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validateJsonPage(JsonObject page, String... expectedKeys) {
        return Arrays.stream(expectedKeys).allMatch(page::containsKey);
    }

    private void apiGetPage(RoutingContext context) {
        int id = Integer.parseInt(context.request().getParam("id"));
        dbService.rxFetchPageById(id)
                .subscribe(apiSingleObserver(context, row -> {
                    if (row == null) {
                        return new ApiResponse(404, new JsonObject()
                                .put("success", false)
                                .put("error", "page " + id + " not found"));
                    }
                    String content = row.getString("content");
                    JsonObject page = new JsonObject()
                            .put("id", id)
                            .put("name", row.getString("name"))
                            .put("markdown", content)
                            .put("html", Processor.process(content));
                    return new ApiResponse(new JsonObject()
                            .put("success", true)
                            .put("page", page));
                }));
    }

    private void apiRoot(RoutingContext context) {
        dbService.rxFetchAllPagesData()
                .map(rows -> rows.stream()
                        .map(row -> new JsonObject()
                                .put("id", row.getInteger("ID"))
                                .put("name", row.getString("NAME")))
                        .collect(Collectors.toList()))
                .subscribe(apiSingleObserver(context, pages -> new ApiResponse(new JsonObject()
                        .put("success", true)
                        .put("pages", pages))));
    }

    private void apiReply(RoutingContext context, int statusCode, JsonObject payload) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(payload.encode());
    }

    private static class ApiResponse {
        final int statusCode;
        final JsonObject payload;

        ApiResponse(JsonObject payload) {
            this(200, payload);
        }

        ApiResponse(int statusCode, JsonObject payload) {
            this.statusCode = statusCode;
            this.payload = payload;
        }
    }

    private CompletableObserver apiCompletableObserver(RoutingContext context, int statusCode) {
        return new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                apiReply(context, statusCode, new JsonObject()
                        .put("success", true));
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("failed to fulfill request", e);
                apiReply(context, 500, new JsonObject()
                        .put("success", false)
                        .put("error", e.getMessage()));
            }
        };
    }

    private <T> SingleObserver<T> apiSingleObserver(RoutingContext context, Function<T, ApiResponse> f) {
        return new SingleObserver<>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(T t) {
                ApiResponse response = f.apply(t);
                apiReply(context, response.statusCode, response.payload);
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("failed to fulfill request", e);
                apiReply(context, 500, new JsonObject()
                        .put("success", false)
                        .put("error", e.getMessage()));
            }
        };
    }

    private void backupHandler(RoutingContext context) {
        dbService.rxFetchAllPagesData()
                .map(rows -> rows.stream()
                        .map(row -> new JsonObject()
                                .put("name", row.getString("NAME"))
                                .put("content", row.getString("CONTENT"))
                        ).collect(JsonArray::new, JsonArray::add, JsonArray::addAll))
                .flatMap(pages -> {
                    JsonObject payload = new JsonObject()
                            .put("files", pages)
                            .put("language", "plaintext")
                            .put("title", "vertx-wiki-backup")
                            .put("public", true);
                    return webClient.post(443, "snippets.glot.io", "/snippets")
                            .putHeader("Content-Type", "application/json")
                            .as(BodyCodec.jsonObject())
                            .rxSendJsonObject(payload);
                })
                .subscribe(singleObserver(context, response -> {
                    JsonObject body = response.body();
                    if (response.statusCode() == 200) {
                        String id = body.getString("id");
                        context.put("backup_gist_url", "https://glot.io/snippets/" + id);
                        // redirect?
                        indexHandler(context);
                    } else {
                        StringBuilder messageBuilder = new StringBuilder()
                                .append("Could not backup the wiki: ")
                                .append(response.statusMessage());
                        if (body != null) {
                            messageBuilder
                                    .append("\n")
                                    .append(body.encodePrettily());
                        }
                        LOGGER.error(messageBuilder.toString());
                        context.fail(502);
                    }
                }));
    }

    private enum PageDeletionResult {
        UNAUTHORIZED,
        DONE
    }

    private void pageDeleteHandler(RoutingContext context) {
        context.user().rxIsAuthorized("delete")
                .flatMap(canDelete -> {
                    if (!canDelete) {
                        return Single.just(PageDeletionResult.UNAUTHORIZED);
                    }
                    String id = context.request().getParam("id");
                    return dbService.rxDeletePage(Integer.parseInt(id))
                            .toSingleDefault(PageDeletionResult.DONE);
                })
                .subscribe(singleObserver(context, result -> {
                    HttpServerResponse response = context.response();
                    if (result == PageDeletionResult.UNAUTHORIZED) {
                        response.setStatusCode(403)
                                .end();
                    } else {
                        response.setStatusCode(303)
                                .putHeader("Location", "/")
                                .end();
                    }
                }));
    }

    private void pageSaveHandler(RoutingContext context) {
        HttpServerRequest request = context.request();
        String page = request.getParam("title");
        String markdown = request.getParam("markdown");
        boolean newPage = "yes".equals(request.getParam("newPage"));

        if (newPage) {
            dbService.rxCreatePage(page, markdown)
                    .subscribe(redirectObserver(context, "/wiki/" + page));
        } else {
            int id = Integer.parseInt(request.getParam("id"));
            dbService.rxSavePage(id, markdown)
                    .subscribe(redirectObserver(context, "/wiki/" + page));
        }
    }

    private void pageCreateHandler(RoutingContext context) {
        String page = context.request().getParam("name");
        String location = "/wiki/" + page;
        if (page == null || page.isEmpty()) {
            location = "/";
        }
        context.response()
                .setStatusCode(303)
                .putHeader("Location", location)
                .end();
    }

    private void pageRenderingHandler(RoutingContext context) {
        String page = context.request().getParam("page");
        dbService.rxFetchPage(page)
                .flatMap(json -> {
                    String rawContent;
                    if (json != null) {
                        rawContent = json.getString("content");
                        context.put("id", json.getInteger("id"));
                        context.put("newPage", "no");
                    } else {
                        rawContent = EMPTY_PAGE_MARKDOWN;
                        context.put("id", -1);
                        context.put("newPage", "yes");
                    }
                    context.put("title", page);
                    context.put("rawContent", rawContent);
                    context.put("content", Processor.process(rawContent));
                    context.put("timestamp", new Date().toString());
                    return templateEngine.rxRender(context.data(), "templates/page.ftl");
                })
                .subscribe(renderHtmlObserver(context));
    }

    private void indexHandler(RoutingContext context) {
        context.user().rxIsAuthorized("create")
                .flatMap(canCreate -> {
                    context.put("canCreatePage", canCreate);
                    return dbService.rxFetchAllPages();
                })
                .flatMap(pages -> {
                    context.put("title", "Wiki home");
                    context.put("username", context.user().principal().getString("username"));
                    context.put("pages", pages.getList());
                    return templateEngine.rxRender(context.data(), "templates/index.ftl");
                })
                .subscribe(renderHtmlObserver(context));
    }

    private static void redirect(RoutingContext context, String location) {
        context.response()
                .setStatusCode(303)
                .putHeader("Location", location)
                .end();
    }

    private static CompletableObserver redirectObserver(final RoutingContext context, final String location) {
        return new CompletableObserver() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onComplete() {
                redirect(context, location);
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("failed to fulfill request", e);
                context.fail(e);
            }
        };
    }

    private static SingleObserver<Buffer> renderHtmlObserver(final RoutingContext context) {
        return singleObserver(context, buffer -> context.response()
                .putHeader("Content-Type", "text/html")
                .end(buffer));
    }

    private static <T> SingleObserver<T> singleObserver(final RoutingContext context, Consumer<T> f) {
        return new SingleObserver<>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(T t) {
                f.accept(t);
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.error("failed to fulfill request", e);
                context.fail(e);
            }
        };
    }
}
