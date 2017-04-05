package com.bamboo.auth;

import java.util.Date;

public final class AuthTokenObject {
	private final String rootObjectId; 
	private final Date expireBy;
	
	public AuthTokenObject(String rootObjectId, Date expireBy) {
		this.rootObjectId = rootObjectId;
		this.expireBy = new Date(expireBy.getTime());
	}

	public String getRootObjectId() {
		return rootObjectId;
	}

	public Date getExpireBy() {
		return new Date(expireBy.getTime());
	}
	
}
