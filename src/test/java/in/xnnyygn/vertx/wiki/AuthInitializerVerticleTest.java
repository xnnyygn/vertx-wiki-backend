package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.DatabaseConstants;
import in.xnnyygn.vertx.wiki.reactivex.VertxUtils;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class AuthInitializerVerticleTest {
    @Test
    public void test() {
        Vertx vertx = Vertx.vertx();

        JsonObject conf = new JsonObject()
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
                .put(DatabaseConstants.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(conf);
        VertxUtils.rxDeployVerticle(vertx, new AuthInitializerVerticle(), deploymentOptions)
                .ignoreElement()
                .blockingAwait();

        VertxUtils.rxClose(vertx)
                .blockingAwait();
    }
}
