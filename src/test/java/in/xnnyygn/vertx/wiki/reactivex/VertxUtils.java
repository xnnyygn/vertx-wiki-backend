package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class VertxUtils {
    public static Single<String> rxDeployVerticle(Vertx vertx, Verticle verticle) {
        return SourceUtils.toSingle(h -> vertx.deployVerticle(verticle, new DeploymentOptions(), h));
    }

    public static Single<String> rxDeployVerticle(Vertx vertx, Verticle verticle, DeploymentOptions options) {
        return SourceUtils.toSingle(h -> vertx.deployVerticle(verticle, options, h));
    }

    public static Completable rxClose(Vertx vertx) {
        return SourceUtils.toCompletable(vertx::close);
    }
}
