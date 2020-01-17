package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.DatabaseConstants;
import in.xnnyygn.vertx.wiki.database.WikiDatabaseVerticle;
import in.xnnyygn.vertx.wiki.reactivex.HttpClientUtils;
import in.xnnyygn.vertx.wiki.reactivex.VertxUtils;
import in.xnnyygn.vertx.wiki.reactivex.WebClientUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
public class HttpServerVerticleHttpsTest {

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

        JksOptions jksOptions = new JksOptions()
                .setPath("server-keystore.jks")
                .setPassword("secret");
        WebClientOptions clientOptions = new WebClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8080)
                .setSsl(true)
                .setTrustOptions(jksOptions);
        client = WebClient.create(vertx, clientOptions);
    }

    @Test
    public void testRoot() {
        HttpRequest<Buffer> request = client.get("/");
        HttpResponse<Buffer> response = WebClientUtils.rxSend(request).blockingGet();
        assertEquals(200, response.statusCode());
    }

    @Test
    public void testRoot2() {
        JksOptions jksOptions = new JksOptions()
                .setPath("server-keystore.jks")
                .setPassword("secret");
        HttpClientOptions clientOptions = new HttpClientOptions()
                .setSsl(true)
                .setTrustOptions(jksOptions);
        HttpClient client = vertx.createHttpClient(clientOptions);
        HttpClientResponse response = HttpClientUtils.rxGetNow(client, 8080, "localhost", "/").blockingGet();
        assertEquals(200, response.statusCode());
        Buffer body = HttpClientUtils.rxBody(response).blockingGet();
        assertTrue(body.length() > 0);
    }

    @After
    public void tearDown() {
        VertxUtils.rxClose(vertx).blockingAwait();
    }
}
