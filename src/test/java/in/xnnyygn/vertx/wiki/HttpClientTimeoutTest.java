package in.xnnyygn.vertx.wiki;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.ConnectException;

@SuppressWarnings("BeforeOrAfterWithIncorrectSignature")
@RunWith(VertxUnitRunner.class)
public class HttpClientTimeoutTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext tc) {
        VertxOptions options = new VertxOptions();
        vertx = Vertx.vertx(options);
    }

    @Test(expected = ConnectException.class)
    public void test(TestContext tc) {
        Async async = tc.async();
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(8080, server -> sendRequest(tc, async));
        async.await();
    }

    private void sendRequest(TestContext tc, Async async) {
        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout(1000);
        WebClient client = WebClient.create(vertx, options);
        client.get(8081, "localhost", "/")
                .send(r -> handleResponse(tc, async, r));
    }

    private void handleResponse(TestContext tc, Async async, AsyncResult<HttpResponse<Buffer>> asyncResult) {
        if (asyncResult.failed()) {
            tc.fail(asyncResult.cause());
            return;
        }
        HttpResponse<Buffer> response = asyncResult.result();
        tc.assertTrue(response.headers().contains("Content-Type"));
        tc.assertEquals("text/plain", response.getHeader("Content-Type"));
        tc.assertEquals("OK", response.bodyAsString());
        async.complete();
    }

    private void handleRequest(HttpServerRequest r) {
        r.response()
                .putHeader("Content-Type", "text/plain")
                .end("OK");
    }

    @After
    public void tearDown(TestContext tc) {
        vertx.close(tc.asyncAssertSuccess());
    }
}
