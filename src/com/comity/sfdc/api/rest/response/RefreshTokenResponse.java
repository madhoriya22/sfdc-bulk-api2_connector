package com.comity.sfdc.api.rest.response;

import java.util.Date;

public class RefreshTokenResponse {
	
	public String access_token;
	public String signature;
	public String scope;
	public String instance_url;
	public String id;
	public String token_type;
	public Date issued_at;
	
	/*public RefreshTokenResponse(String accessToken, String instanceUrl) {
		this.accessToken = accessToken;
		this.instanceUrl = instanceUrl;
	}*/
	
	public String getAccessToken() {
		return this.access_token;
	}
	
	public String getSignature() {
		return this.signature;
	}
	
	public String getScope() {
		return this.scope;
	}
	
	public String getInstanceUrl() {
		return this.instance_url;
	}
	
	public String getId() {
		return this.id;
	}

	public String getTokenType() {
		return this.token_type;
	}
	
	public Date getIssuedAt() {
		return this.issued_at;
	}
}
