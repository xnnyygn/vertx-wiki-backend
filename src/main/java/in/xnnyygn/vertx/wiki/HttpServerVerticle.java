package in.xnnyygn.vertx.wiki;

import com.github.rjeschke.txtmark.Processor;
import in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  private static final String EMPTY_PAGE_MARKDOWN = "# A new page\n" +
    "\n" +
    "Feel-free to write in Markdown!\n";

  private WikiDatabaseService dbService;
  private FreeMarkerTemplateEngine templateEngine;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    String wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
    dbService = in.xnnyygn.vertx.wiki.database.WikiDatabaseService.createProxy(vertx.getDelegate(), wikiDbQueue);

    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.post().handler(BodyHandler.create());
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/save").handler(this::pageSaveHandler);
    router.post("/delete").handler(this::pageDeleteHandler);

    templateEngine = FreeMarkerTemplateEngine.create(vertx);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);

    server
      .requestHandler(router)
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

  private void pageDeleteHandler(RoutingContext context) {
    String id = context.request().getParam("id");
    dbService.rxDeletePage(Integer.parseInt(id))
      .subscribe(redirectObserver(context, "/"));
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
        boolean found = json.getBoolean("found");
        String rawContent = json.getString("content", EMPTY_PAGE_MARKDOWN);
        if (found) {
          context.put("id", json.getInteger("id"));
          context.put("newPage", "no");
        } else {
          context.put("id", -1);
          context.put("newPage", "yes");
        }
        context.put("title", page);
        context.put("rawContent", rawContent);
        context.put("content", Processor.process(rawContent));
        context.put("timestamp", new Date().toString());
        return templateEngine.rxRender(context.data(), "templates/page.ftl");
      }).subscribe(renderHtmlObserver(context));
  }

  private void indexHandler(RoutingContext context) {
    dbService.rxFetchAllPages()
      .flatMap(pages -> {
        context.put("title", "Wiki home");
        context.put("pages", pages.getList());
        return templateEngine.rxRender(context.data(), "templates/index.ftl");
      }).subscribe(renderHtmlObserver(context));
  }

  private static DisposableCompletableObserver redirectObserver(final RoutingContext context, final String location) {
    return new DisposableCompletableObserver() {
      @Override
      public void onComplete() {
        context.response()
          .setStatusCode(303)
          .putHeader("Location", location)
          .end();
        dispose();
      }

      @Override
      public void onError(Throwable e) {
        LOGGER.error("failed to fulfill request", e);
        context.fail(e);
        dispose();
      }
    };
  }

  private static DisposableSingleObserver<Buffer> renderHtmlObserver(final RoutingContext context) {
    return new DisposableSingleObserver<Buffer>() {
      @Override
      public void onSuccess(Buffer buffer) {
        context.response()
          .putHeader("Content-Type", "text/html")
          .end(buffer);
        dispose();
      }

      @Override
      public void onError(Throwable e) {
        LOGGER.error("failed to fulfill request", e);
        context.fail(e);
        dispose();
      }
    };
  }
}
