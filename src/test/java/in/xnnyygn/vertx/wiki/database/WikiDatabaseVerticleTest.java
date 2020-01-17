package in.xnnyygn.vertx.wiki.database;

import in.xnnyygn.vertx.wiki.reactivex.VertxUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class WikiDatabaseVerticleTest {

    private Vertx vertx;
    private in.xnnyygn.vertx.wiki.database.reactivex.WikiDatabaseService service;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        JsonObject conf = new JsonObject()
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(conf);
        service = VertxUtils.rxDeployVerticle(vertx, new WikiDatabaseVerticle(), deploymentOptions)
                .map(id -> WikiDatabaseService.createProxy(vertx, DatabaseConstants.CONFIG_WIKIDB_QUEUE))
                .blockingGet();
    }

    @After
    public void tearDown() {
        VertxUtils.rxClose(vertx)
                .blockingAwait();
    }

    @Test
    public void testFetchPageById() {
        JsonObject row = service.rxFetchPageById(1).blockingGet();
        assertNull(row);
    }

    @Test
    public void testCrud() {
        service.rxCreatePage("Test", "Some Content").blockingAwait();

        JsonObject page = service.rxFetchPage("Test").blockingGet();
        assertTrue(page.getBoolean("found"));
        assertTrue(page.containsKey("id"));
        assertEquals("Some Content", page.getString("content"));
        Integer pageId = page.getInteger("id");

        service.rxSavePage(pageId, "Yo!").blockingAwait();

        service.rxDeletePage(pageId).blockingAwait();
    }

    @Test
    public void testUpdate() {
        service.rxCreatePage("Test", "Some Content").blockingAwait();

        JsonObject page = service.rxFetchPage("Test").blockingGet();
        Integer pageId = page.getInteger("id");

        service.rxSavePage(pageId, "Yo!").blockingAwait();
        page = service.rxFetchPage("Test").blockingGet();
        assertEquals("Yo!", page.getString("content"));
    }

    @Test
    public void testWikiNotFound() {
        JsonObject page = service.rxFetchPage("Test2").blockingGet();
        assertNull(page);
    }

    @Test
    public void testFetchAllPagesData() {
        service.rxCreatePage("Test", "Some Content").blockingAwait();
        List<JsonObject> pages = service.rxFetchAllPagesData().blockingGet();
        assertEquals(1, pages.size());
    }
}
