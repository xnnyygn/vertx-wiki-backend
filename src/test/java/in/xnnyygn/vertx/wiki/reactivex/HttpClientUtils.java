package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class HttpClientUtils {
    public static Single<HttpClientResponse> rxGetNow(HttpClient client, int port, String host, String requestURI) {
        return Single.create(emitter -> {
            HttpClientRequest request = client.get(port, host, requestURI);
            //noinspection deprecation
            request.handler(emitter::onSuccess);
            request.exceptionHandler(emitter::onError);
            request.end();
        });
    }

    public static Single<Buffer> rxBody(HttpClientResponse response) {
        return Single.create(emitter -> {
            response.bodyHandler(emitter::onSuccess);
            response.exceptionHandler(emitter::onError);
        });
    }
}
