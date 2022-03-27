package com.github.brane08.pagila.seedworks.beans;

import java.io.Serializable;
import java.util.List;

public class ApiResult<T> implements Serializable {

    protected final boolean success;
    protected final T data;

    public ApiResult(T data) {
        this(true, data);
    }

    public ApiResult(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static <R> ApiResult<R> single(R obj) {
        return new ApiResult<>(true, obj);
    }

    public static ApiResult<CountInfo> count(long total) {
        return new ApiResult<>(true, new CountInfo(total));
    }

    public static <R> ApiResult<List<R>> array(List<R> list) {
        return array(list, 0);
    }

    public static <R> ApiResult<List<R>> array(List<R> list, int totalCount) {
        return new PageResult<>(list, totalCount);
    }

    public static <R> ApiResult<List<R>> array(PagedList<R> list) {
        return new PageResult<>(list.list(), list.totalCount());
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }
}
