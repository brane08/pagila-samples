package com.github.brane08.pagila.seedworks.entities;

import java.util.List;

public class PagedList<T> {

	private final List<T> list;
	private final int totalCount;
	private final int totalPageCount;
	private final boolean hasPrev;
	private final int pageIndex;

	public PagedList(List<T> list, int totalCount, int totalPageCount, boolean hasPrev, int pageIndex) {
		this.list = list;
		this.totalCount = totalCount;
		this.totalPageCount = totalPageCount;
		this.hasPrev = hasPrev;
		this.pageIndex = pageIndex;
	}

	public List<T> getList() {
		return list;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public int getTotalPageCount() {
		return totalPageCount;
	}

	public boolean isHasPrev() {
		return hasPrev;
	}

	public int getPageIndex() {
		return pageIndex;
	}
}
