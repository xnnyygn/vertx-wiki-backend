package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

public class WebClientUtils {
    public static Single<HttpResponse<Buffer>> rxSend(HttpRequest<Buffer> request) {
        return SingleUtils.toSingle(request, HttpRequest::send);
    }

    public static Single<HttpResponse<Buffer>> rxSendJsonObject(HttpRequest<Buffer> request, JsonObject payload) {
        return SingleUtils.toSingle(request, (r, h) -> r.sendJsonObject(payload, h));
    }
}
