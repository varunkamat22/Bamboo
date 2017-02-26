package com.bamboo.core;

import java.util.List;

public class ListResponse {

	private int totalCount;
	private List results;

	public ListResponse(List results) {
		this.results = results;
		totalCount = results == null ? 0 : results.size();
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public List getSearchResult() {
		return results;
	}

	public void setSearchResult(List results) {
		this.results = results;
	}

}
