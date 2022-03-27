package com.github.brane08.pagila.seedworks.beans;

import java.util.Map;

public final class PageRequest {

	public final int page;
	public final int size;
	public final String order;
	public final String filter;

	private PageRequest(int page, int size, String order, String filter) {
		this.page = page;
		this.size = size;
		this.order = order;
		this.filter = filter;
	}

	public static PageRequest from(int page, int size, String order, String filter) {
		return new PageRequest(page, size, order, filter);
	}

	private static String getFirstValue(String key, Map<String, String[]> data) {
		String[] values = data.getOrDefault(key, new String[]{});
		return values.length > 0 ? values[0] : "";
	}

	public int offset() {
		return page * size;
	}
}
