package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.BiConsumer;

public class SingleUtils {
    public static <T, R> Single<R> toSingle(T t,
                                            BiConsumer<T, Handler<AsyncResult<R>>> f) {
        return Single.create(emitter -> f.accept(t, asyncResponse -> {
            if (asyncResponse.succeeded()) {
                emitter.onSuccess(asyncResponse.result());
            } else {
                emitter.onError(asyncResponse.cause());
            }
        }));
    }
}
