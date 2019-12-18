package in.xnnyygn.vertx.wiki;

import in.xnnyygn.vertx.wiki.database.WikiDatabaseVerticle;
import io.reactivex.observers.DisposableSingleObserver;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.reactivex.core.AbstractVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.rxDeployVerticle(WikiDatabaseVerticle.class.getName())
      .flatMap(id -> vertx.rxDeployVerticle(HttpServerVerticle.class.getName(), new DeploymentOptions().setInstances(2)))
      .subscribe(deploymentObserver(startPromise));
  }

  private DisposableSingleObserver<String> deploymentObserver(Promise<Void> startPromise) {
    return new DisposableSingleObserver<String>() {
      @Override
      public void onSuccess(String s) {
        startPromise.complete();
        dispose();
      }

      @Override
      public void onError(Throwable e) {
        startPromise.fail(e);
        dispose();
      }
    };
  }

  public static void main(String[] args) {
    Vertx vertex = Vertx.vertx();
    vertex.deployVerticle(new MainVerticle());
  }
}
