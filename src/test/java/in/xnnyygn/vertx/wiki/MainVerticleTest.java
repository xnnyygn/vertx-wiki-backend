package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.WikiDatabaseVerticle;
import in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("BeforeOrAfterWithIncorrectSignature")
@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private Vertx vertx;
  private WikiDatabaseService service;

  @Before
  public void setUp(TestContext tc) {
    vertx = Vertx.vertx();

    JsonObject conf = new JsonObject()
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

    vertx.deployVerticle(
      new WikiDatabaseVerticle(),
      new DeploymentOptions().setConfig(conf),
      tc.asyncAssertSuccess(id -> {
        service = in.xnnyygn.vertx.wiki.database.WikiDatabaseService.createProxy(vertx, WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE);
      })
    );
  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }

  @Test
  public void testCrud(TestContext tc) {
    service.rxCreatePage("Test", "Some Content")
      .andThen(Single.defer(() -> service.rxFetchPage("Test")))
      .flatMap(json -> {
        tc.assertTrue(json.getBoolean("found"));
        tc.assertTrue(json.containsKey("id"));
        tc.assertEquals("Some Content", json.getString("content"));
        return Single.just(json.getInteger("id"));
      })
      .flatMap(id -> service.rxSavePage(id, "Yo!").andThen(Single.just(id)))
      .flatMapCompletable(id -> service.rxDeletePage(id))
      .subscribe(CompletableHelper.toObserver(tc.asyncAssertSuccess()));
  }

  @Test
  public void testUpdate(TestContext tc) {
    service.rxCreatePage("Test", "Some Content")
      .andThen(Single.defer(() -> service.rxFetchPage("Test")))
      .flatMap(json -> Single.just(json.getInteger("id")))
      .flatMapCompletable(id -> service.rxSavePage(id, "Yo!"))
      .andThen(Single.defer(() -> service.rxFetchPage("Test")))
      .subscribe(SingleHelper.toObserver(tc.asyncAssertSuccess(json ->
        tc.assertEquals("Yo!", json.getString("content"))
      )));
  }

  @Test
  public void testWikiNotFound(TestContext tc) {
    service.rxFetchPage("Test2")
      .subscribe(SingleHelper.toObserver(tc.asyncAssertSuccess(json ->
        tc.assertFalse(json.getBoolean("found"))
      )));
  }

  @Test
  @Ignore
  public void testThatTheServerIsStarted(TestContext tc) {
    Async async = tc.async();
    vertx.createHttpClient().getNow(8080, "localhost", "/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        async.complete();
      });
    });
  }

}
