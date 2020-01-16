package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class VertxUtils {

    public static Single<String> rxDeployVerticle(Vertx vertx, Verticle verticle) {
        return SingleUtils.toSingle(vertx, (v, h) -> v.deployVerticle(verticle, new DeploymentOptions(), h));
    }

    public static Single<String> rxDeployVerticle(Vertx vertx, Verticle verticle, DeploymentOptions options) {
        return SingleUtils.toSingle(vertx, (v, h) -> v.deployVerticle(verticle, options, h));
    }

    public static Completable rxClose(Vertx vertx) {
        return Completable.create(emitter -> vertx.close(asyncResult -> {
            if (asyncResult.succeeded()) {
                emitter.onComplete();
            } else {
                emitter.onError(asyncResult.cause());
            }
        }));
    }
}
