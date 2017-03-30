package com.bamboo.core.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.http.HttpStatus;
import com.bamboo.core.HttpError;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class HttpErrorHelperUtil {
	
	private HttpErrorHelperUtil(){}
	
	public static Response getResourceNotFoundResponse(String resourceName) {
		return Response.status(HttpStatus.NOT_FOUND.value())
				.entity(new Gson().toJson(new HttpError(String.format(ResourceConstants.RESOURCE_NOT_FOUND, resourceName))))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.build();
	}

	public static Response getSuccessResponse(Object entity) {
		Gson gson = new Gson();
		return Response.status(HttpStatus.OK.value())
					   .entity(gson.toJson(entity))
					   .header("Content-Type", MediaType.APPLICATION_JSON)
					   .build();
	}
	
	public static Response getCreationSuccessResponse(Object entity){
		Gson gson = new Gson();
		return Response.status(HttpStatus.CREATED.value())
					   .entity(gson.toJson(entity))
					   .header("Content-Type", MediaType.APPLICATION_JSON)
					   .build();
	}

	public static Response getBadRequestResponse(String... messages) {
		JsonObject errorObject = new JsonObject();
		JsonArray errors = new JsonArray();
		for(String msg : messages){
			errors.add(msg);
		}
		errorObject.add(ResourceConstants.ERROR_ROOT_TAG, errors);
		return Response.status(HttpStatus.BAD_REQUEST.value())
					   .entity(errorObject.toString())
					   .header("Content-Type", MediaType.APPLICATION_JSON)
					   .build();
	}
	
	public static Response getNoContentResponse(){
		return Response.status(HttpStatus.NO_CONTENT.value()).build();
	}
	
	public static Response getUnknownIDResponse(String id){
		return Response.status(HttpStatus.NOT_FOUND.value())
					   .entity(new Gson().toJson(new HttpError(String.format(ResourceConstants.UNKNOWN_ID, id))))
					   .header("Content-Type", MediaType.APPLICATION_JSON)
					   .build();
	}
	
	public static Response getServerErrorResponse(String message){
		return Response.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.entity(new Gson().toJson(new HttpError(message)))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.build();
	}
	
	public static Response getUnauthorizedResponse(){
		return Response.status(HttpStatus.UNAUTHORIZED.value())
				.entity(new Gson().toJson(new HttpError("Authorization failed for your request")))
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.build();
	}
	
}
