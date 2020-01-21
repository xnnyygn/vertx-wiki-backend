package in.xnnyygn.vertx.wiki;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.vertx.reactivex.ext.sql.SQLConnection;

public class SQLConnectionUtils {
    public static CompletableTransformer closeCompletable(SQLConnection conn) {
        return upstream -> upstream
                .onErrorResumeNext(e -> conn.rxClose().andThen(Completable.error(e)))
                .andThen(Completable.defer(conn::rxClose));
    }
}
