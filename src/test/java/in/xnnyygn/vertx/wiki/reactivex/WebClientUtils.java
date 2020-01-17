package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

public class WebClientUtils {
    public static Single<HttpResponse<Buffer>> rxSend(HttpRequest<Buffer> request) {
        return SourceUtils.toSingle(request::send);
    }

    public static Single<HttpResponse<Buffer>> rxSendJsonObject(HttpRequest<Buffer> request, JsonObject payload) {
        return SourceUtils.toSingle(h -> request.sendJsonObject(payload, h));
    }
}
