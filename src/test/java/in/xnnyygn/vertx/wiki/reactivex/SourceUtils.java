package in.xnnyygn.vertx.wiki.reactivex;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SourceUtils {
    public static <R> Single<R> toSingle(Consumer<Handler<AsyncResult<R>>> f) {
        return Single.create(emitter -> f.accept(asyncResult -> {
            if (asyncResult.succeeded()) {
                emitter.onSuccess(asyncResult.result());
            } else {
                emitter.onError(asyncResult.cause());
            }
        }));
    }

    public static <T, R> Single<R> toSingle(T t,
                                            BiConsumer<T, Handler<AsyncResult<R>>> f) {
        return Single.create(emitter -> f.accept(t, asyncResult -> {
            if (asyncResult.succeeded()) {
                emitter.onSuccess(asyncResult.result());
            } else {
                emitter.onError(asyncResult.cause());
            }
        }));
    }

    public static Completable toCompletable(Consumer<Handler<AsyncResult<Void>>> f) {
        return Completable.create(emitter -> f.accept(asyncResult -> {
            if (asyncResult.succeeded()) {
                emitter.onComplete();
            } else {
                emitter.onError(asyncResult.cause());
            }
        }));
    }
}
