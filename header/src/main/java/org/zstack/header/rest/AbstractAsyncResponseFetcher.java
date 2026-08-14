package org.zstack.header.rest;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class AbstractAsyncResponseFetcher<T> {
    public final Class<T> returnType;
    protected Supplier<RestHttp<String>> restHttpFactory;
    protected List<Consumer<RestHttp<String>>> restHttpDecorators = new ArrayList<>();

    protected Function<String, T> resultsBuilder;
    protected Predicate<T> donePredicate;

    protected long eachAttemptTimeoutInMillis = 3000L;
    protected long eachAttemptIntervalInMillis = 1000L;
    protected long allAttemptsTimeoutInMillis = 60000L;

    {
        restHttpDecorators.add(http -> http.withTimeoutInMillis(eachAttemptTimeoutInMillis));
    }

    protected static final ErrorCode NOT_FINISHED_YET;

    static {
        NOT_FINISHED_YET = new ErrorCode();
        NOT_FINISHED_YET.setCode("AsyncResponseFetcher.NOT_FINISHED_YET");
        NOT_FINISHED_YET.setDetails("Not finished yet");
    }

    protected AbstractAsyncResponseFetcher(Class<T> returnType) {
        this.returnType = returnType;
        this.resultsBuilder = v -> JSONObjectUtil.toObject(v, this.returnType);
    }

    public AbstractAsyncResponseFetcher<T> withRestHttpFactory(Supplier<RestHttp<String>> restHttpFactory) {
        this.restHttpFactory = restHttpFactory;
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withRestHttpDecorator(Consumer<RestHttp<String>> restHttpDecorator) {
        this.restHttpDecorators.add(restHttpDecorator);
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withResultsBuilder(Function<String, T> resultsBuilder) {
        this.resultsBuilder = resultsBuilder;
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withDonePredicate(Predicate<T> donePredicate) {
        this.donePredicate = donePredicate;
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withEachAttemptTimeoutInMillis(long eachAttemptTimeoutInMillis) {
        this.eachAttemptTimeoutInMillis = eachAttemptTimeoutInMillis;
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withEachAttemptIntervalInMillis(long eachAttemptIntervalInMillis) {
        this.eachAttemptIntervalInMillis = eachAttemptIntervalInMillis;
        return this;
    }

    public AbstractAsyncResponseFetcher<T> withAllAttemptsTimeoutInMillis(long allAttemptsTimeoutInMillis) {
        this.allAttemptsTimeoutInMillis = allAttemptsTimeoutInMillis;
        return this;
    }

    protected RestHttp<String> http() {
        return restHttpFactory.get();
    }

    public abstract void fetch(ReturnValueCompletion<T> completion);

    protected ErrorableValue<T> eachAttempt() {
        final RestHttp<String> http = http();
        for (Consumer<RestHttp<String>> restHttpDecorator : restHttpDecorators) {
            restHttpDecorator.accept(http);
        }

        ErrorableValue<String> value = http.getWithErrorCode();
        if (!value.isSuccess()) {
            return value.cast();
        }

        T result = resultsBuilder.apply(value.result);
        if (donePredicate != null && !donePredicate.test(result)) {
            return ErrorableValue.ofErrorCode(NOT_FINISHED_YET);
        }

        return ErrorableValue.of(result);
    }
}
