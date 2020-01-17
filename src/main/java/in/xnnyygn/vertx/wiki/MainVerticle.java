package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.WikiDatabaseVerticle;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {
    @Override
    public Completable rxStart() {
        return vertx.rxDeployVerticle(WikiDatabaseVerticle.class.getName())
                .flatMap(id -> vertx.rxDeployVerticle(AuthInitializerVerticle.class.getName()))
                .flatMap(id -> vertx.rxDeployVerticle(HttpServerVerticle.class.getName(), new DeploymentOptions()
                        .setInstances(2)))
                .ignoreElement();
    }

    public static void main(String[] args) {
        Vertx vertex = Vertx.vertx();
        vertex.deployVerticle(new MainVerticle());
    }
}
