package com.github.brane08.pagila.seedworks.beans;

import com.github.brane08.pagila.seedworks.entities.PagedList;

import java.io.Serializable;
import java.util.List;

public class ApiResult<T> implements Serializable {

	protected final boolean success;
	protected final T data;

	public ApiResult(boolean success, T data) {
		this.success = success;
		this.data = data;
	}

	public boolean isSuccess() {
		return success;
	}

	public T getData() {
		return data;
	}

	public static <R> ApiResult<R> single(R obj) {
		return new ApiResult<>(true, obj);
	}

	public static <R> ApiResult<List<R>> array(List<R> list, int totalCount) {
		return new PageResult<>(true, list, totalCount);
	}
	public static <R> ApiResult<List<R>> array(PagedList<R> paged) {
		return new PageResult<>(true, paged.getList(), paged.getTotalCount());
	}
}
