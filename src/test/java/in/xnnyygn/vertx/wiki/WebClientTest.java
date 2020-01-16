package in.xnnyygn.vertx.wiki;

import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SuppressWarnings("BeforeOrAfterWithIncorrectSignature")
@RunWith(VertxUnitRunner.class)
public class WebClientTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext tc) {
        vertx = Vertx.vertx();
    }

    @Test
    public void test(TestContext tc) {
        Async async = tc.async();
        vertx.createHttpServer()
                .requestHandler(this::handleRequest)
                .listen(8080, server -> sendRequest(tc, async));
        async.await(5000);
    }

    private void sendRequest(TestContext tc, Async async) {
        WebClient client = WebClient.create(vertx);
        client.get(8080, "localhost", "/")
                .send(r -> handleResponse(tc, async, r));
    }

    private void handleResponse(TestContext tc, Async async, AsyncResult<HttpResponse<Buffer>> asyncResult) {
        if (asyncResult.failed()) {
            Promise<Void> promise = Promise.promise();
            promise.fail(asyncResult.cause());
            async.resolve(promise);
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
