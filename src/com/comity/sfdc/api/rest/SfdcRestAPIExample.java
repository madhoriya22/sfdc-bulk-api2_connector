package com.comity.sfdc.api.rest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import com.comity.sfdc.api.rest.request.CreateJobRequest;
import com.comity.sfdc.api.rest.request.UploadCompleteRequest;
import com.comity.sfdc.api.rest.response.CreateJobResponse;
import com.comity.sfdc.api.rest.response.RefreshTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SfdcRestAPIExample {
	
	private static final String TOKEN_URL =  "https://login.salesforce.com/services/oauth2/token";
	private static final String CLIENT_ID = "<CLIENT_ID_FROM_CONNECTED_APP";
	private static final String CLIENT_SECRET = "<CLIENT_SECRET_FROM_CONNECTED_APP>";
	private static final String GRANT_TYPE = "refresh_token";
	private static final String REFRESH_TOKEN = "<REFRESH_TOKEN_FROM_REST_CALL>";
	private static final String BEARER = "Bearer";
	private static final String CONTENT_TYPE_JSON = "application/json";
	private static final String CONTENT_TYPE_CSV = "text/csv";
	private static final String ACCEPT = "application/json";
	private static final String REST_URI = "/services/data/v42.0/jobs";
	private static final String CREATE_JOB_URI = "/ingest/";
	private static final String UPLOAD_JOB_URI = "/batches/";
	
	public static void main(String args[]) {
		RefreshTokenResponse wrapper = getAccessTokenFromRefreshToken();
		CreateJobResponse createJobResponse = createJob(wrapper);
		uploadDataToJob(wrapper, createJobResponse.id);
		markDataLoadCompleteForJob(wrapper, createJobResponse.id);
	}
	
	private static RefreshTokenResponse getAccessTokenFromRefreshToken() {
		RefreshTokenResponse wrapper = null;
		CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.createDefault();

            final List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
            loginParams.add(new BasicNameValuePair("grant_type", GRANT_TYPE));
            loginParams.add(new BasicNameValuePair("client_id", CLIENT_ID));
            loginParams.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
            loginParams.add(new BasicNameValuePair("refresh_token", REFRESH_TOKEN));

            final HttpPost post = new HttpPost(TOKEN_URL);
            post.setEntity(new UrlEncodedFormEntity(loginParams));

            final HttpResponse loginResponse = httpclient.execute(post);

            // parse
            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
            
            System.out.println("loginResult -> "+loginResult);

            wrapper = mapper.treeToValue(loginResult, RefreshTokenResponse.class);
            System.out.println("wrapper -> "+wrapper);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
        	try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return wrapper;
	}
	
	private static CreateJobResponse createJob(RefreshTokenResponse wrapper) {
		CreateJobResponse response = null;
		CloseableHttpClient httpclient = null;
        try {
        	final String createJobURI = wrapper.getInstanceUrl() + REST_URI + CREATE_JOB_URI;
        	final String authorization = BEARER + " " + wrapper.getAccessToken();

            //create JSON body
            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            final CreateJobRequest request = new CreateJobRequest();
            request.object = "Contact";
            request.contentType = "CSV";
            request.operation = "insert";
            request.lineEnding = "CRLF";
            final String requestJson = mapper.writeValueAsString(request);
            final StringEntity jsonBody = new StringEntity(requestJson);

            //configure Post
            final HttpPost post = new HttpPost(createJobURI);
            
            //set Headers
            post.setHeader("authorization", authorization);
            post.setHeader("content-type", CONTENT_TYPE_JSON);
            post.setHeader("accept", ACCEPT);
            
            //set JSON body
            post.setEntity(jsonBody);
            
            httpclient = HttpClients.createDefault();
            final HttpResponse httpResponse = httpclient.execute(post);

            final JsonNode responseJson = mapper.readValue(httpResponse.getEntity().getContent(), JsonNode.class);
            response = mapper.treeToValue(responseJson, CreateJobResponse.class);
            System.out.println("response -> "+response);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return response;
	}
	
	private static void uploadDataToJob(RefreshTokenResponse wrapper, String jobId) {
		CloseableHttpClient httpclient = null;
        try {
        	final String uploadJobUri = wrapper.getInstanceUrl() + REST_URI + CREATE_JOB_URI + jobId + UPLOAD_JOB_URI;
        	final String authorization = BEARER + " " + wrapper.getAccessToken();

            //create JSON body
            File contactCsvFile = new File("data/Bulk API Sample Contact Data.csv");
            HttpEntity fileBody = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE).addBinaryBody("recordCsv", contactCsvFile).build();
            System.out.println("fileBody -> "+fileBody);
            
            //post the request
            final HttpPut put = new HttpPut(uploadJobUri);
            
            //set Headers
            put.setHeader("authorization", authorization);
            put.setHeader("content-type", CONTENT_TYPE_CSV);
            put.setHeader("accept", ACCEPT);
            
            //set CSV file body
            put.setEntity(fileBody);
            
            httpclient = HttpClients.createDefault();
            final HttpResponse response = httpclient.execute(put);

            System.out.println("response -> "+response.getStatusLine().getStatusCode());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	private static void markDataLoadCompleteForJob(RefreshTokenResponse wrapper, String jobId) {
		CloseableHttpClient httpclient = null;
        try {
        	final String createJobURI = wrapper.getInstanceUrl() + REST_URI + CREATE_JOB_URI + jobId;
        	final String authorization = BEARER + " " + wrapper.getAccessToken();

            //create JSON body
            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            final UploadCompleteRequest request = new UploadCompleteRequest();
            request.state = "UploadComplete";
            final String requestJson = mapper.writeValueAsString(request);
            final StringEntity jsonBody = new StringEntity(requestJson);

            //configure Post
            final HttpPost post = new HttpPost(createJobURI);
            
            //set Headers
            post.setHeader("authorization", authorization);
            post.setHeader("content-type", CONTENT_TYPE_JSON);
            post.setHeader("accept", ACCEPT);
            
            //set JSON body
            post.setEntity(jsonBody);
            
            httpclient = HttpClients.createDefault();
            final HttpResponse httpResponse = httpclient.execute(post);

            final JsonNode responseJson = mapper.readValue(httpResponse.getEntity().getContent(), JsonNode.class);
            System.out.println("response -> "+responseJson);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        	try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
}


