package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.DatabaseConstants;
import in.xnnyygn.vertx.wiki.database.WikiDatabaseVerticle;
import in.xnnyygn.vertx.wiki.reactivex.VertxUtils;
import in.xnnyygn.vertx.wiki.reactivex.WebClientUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HttpServerVerticleApiTest {

    private Vertx vertx;
    private WebClient client;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();

        JsonObject conf = new JsonObject()
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

        VertxUtils.rxDeployVerticle(vertx, new WikiDatabaseVerticle(), new DeploymentOptions().setConfig(conf))
                .flatMap(id -> VertxUtils.rxDeployVerticle(vertx, new HttpServerVerticle()))
                .ignoreElement()
                .blockingAwait();

        client = WebClient.create(vertx);
    }

    @Test
    public void testApiRootEmpty() {
        HttpRequest<Buffer> request = client.get(8080, "localhost", "/api/pages");
        HttpResponse<Buffer> response = WebClientUtils.rxSend(request).blockingGet();
        assertEquals(200, response.statusCode());
        JsonObject bodyJson = response.bodyAsJsonObject();
        assertTrue(bodyJson.getBoolean("success"));
        assertEquals(0, bodyJson.getJsonArray("pages").size());
    }

    @Test
    public void testApiCreatePageBadRequest() {
        HttpRequest<Buffer> request = client.post(8080, "localhost", "/api/pages")
                .putHeader("Content-Type", "application/json");
        HttpResponse<Buffer> response = WebClientUtils.rxSendJsonObject(request, new JsonObject()).blockingGet();
        assertEquals(400, response.statusCode());
        JsonObject bodyJson = response.bodyAsJsonObject();
        assertFalse(bodyJson.getBoolean("success"));
        assertEquals("bad request payload", bodyJson.getString("error"));
    }

    @Test
    public void testApiCreatePage() {
        HttpRequest<Buffer> request = client.post(8080, "localhost", "/api/pages")
                .putHeader("Content-Type", "application/json");
        JsonObject payload = new JsonObject()
                .put("name", "test")
                .put("content", "#foo");
        HttpResponse<Buffer> response = WebClientUtils.rxSendJsonObject(request, payload).blockingGet();
        assertEquals(201, response.statusCode());
    }

    private void createPage(String name, String content) {
        HttpRequest<Buffer> request = client.post(8080, "localhost", "/api/pages")
                .putHeader("Content-Type", "application/json");
        JsonObject payload = new JsonObject()
                .put("name", name)
                .put("content", content);
        HttpResponse<Buffer> response = WebClientUtils.rxSendJsonObject(request, payload).blockingGet();
        assertEquals(201, response.statusCode());
    }

    @Test
    public void testGetPageNotFound() {
        HttpRequest<Buffer> request = client.get(8080, "localhost", "/api/pages/1");
        HttpResponse<Buffer> response = WebClientUtils.rxSend(request).blockingGet();
        assertEquals(404, response.statusCode());
        JsonObject bodyJson = response.bodyAsJsonObject();
        assertFalse(bodyJson.getBoolean("success"));
    }

    @Test
    public void testApiUpdatePageBadRequest() {
        HttpRequest<Buffer> request = client.put(8080, "localhost", "/api/pages/1")
                .putHeader("Content-Type", "application/json");
        HttpResponse<Buffer> response = WebClientUtils.rxSendJsonObject(request, new JsonObject()).blockingGet();
        assertEquals(400, response.statusCode());
        JsonObject bodyJson = response.bodyAsJsonObject();
        assertFalse(bodyJson.getBoolean("success"));
        assertEquals("bad request payload", bodyJson.getString("error"));
    }

    @Test
    public void testAll() {
        createPage("foo", "#foo");

        HttpRequest<Buffer> apiRootRequest = client.get(8080, "localhost", "/api/pages");
        HttpResponse<Buffer> apiRootResponse = WebClientUtils.rxSend(apiRootRequest).blockingGet();
        assertEquals(200, apiRootResponse.statusCode());
        JsonObject apiRootBodyJson = apiRootResponse.bodyAsJsonObject();
        assertTrue(apiRootBodyJson.getBoolean("success"));
        JsonArray pages = apiRootBodyJson.getJsonArray("pages");
        assertEquals(1, pages.size());
        JsonObject page0 = pages.getJsonObject(0);
        assertEquals("foo", page0.getString("name"));
        int pageId = page0.getInteger("id");

        HttpRequest<Buffer> apiGetPageRequest = client.get(8080, "localhost", "/api/pages/" + pageId);
        HttpResponse<Buffer> apiGetPageResponse = WebClientUtils.rxSend(apiGetPageRequest).blockingGet();
        assertEquals(200, apiGetPageResponse.statusCode());
        JsonObject apiGetPageBodyJson = apiGetPageResponse.bodyAsJsonObject();
        assertTrue(apiGetPageBodyJson.getBoolean("success"));
        JsonObject page = apiGetPageBodyJson.getJsonObject("page");
        assertEquals("foo", page.getString("name"));
        assertEquals("#foo", page.getString("markdown"));
        assertEquals("<h1>foo</h1>\n", page.getString("html"));

        HttpRequest<Buffer> apiUpdateRequest = client.put(8080, "localhost", "/api/pages/" + pageId);
        HttpResponse<Buffer> apiUpdateResponse = WebClientUtils.rxSendJsonObject(apiUpdateRequest, new JsonObject()
                .put("content", "#bar")).blockingGet();
        assertEquals(200, apiUpdateResponse.statusCode());

        apiGetPageResponse = WebClientUtils.rxSend(apiGetPageRequest).blockingGet();
        assertEquals("#bar", apiGetPageResponse.bodyAsJsonObject().getJsonObject("page").getString("markdown"));

        HttpRequest<Buffer> apiDeleteRequest = client.delete(8080, "localhost", "/api/pages/" + pageId);
        HttpResponse<Buffer> apiDeleteResponse = WebClientUtils.rxSend(apiDeleteRequest).blockingGet();
        assertEquals(204, apiDeleteResponse.statusCode());

        apiGetPageResponse = WebClientUtils.rxSend(apiGetPageRequest).blockingGet();
        assertEquals(404, apiGetPageResponse.statusCode());
    }

    @After
    public void tearDown() {
        VertxUtils.rxClose(vertx).blockingAwait();
    }
}
