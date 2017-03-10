package com.bamboo.core;

import java.util.List;

public class ListResponse<T>{

	private int totalCount;
	private List<T> results;

	public ListResponse(List<T> results) {
		this.results = results;
		totalCount = results == null ? 0 : results.size();
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public List<T> getSearchResult() {
		return results;
	}

	public void setSearchResult(List<T> results) {
		this.results = results;
	}

}
