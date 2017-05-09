package com.bamboo.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.bamboo.core.util.ResourceConstants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.bamboo.auth.AuthenticationHandler;
import com.bamboo.core.util.FilterTranslatorUtil;
import com.bamboo.core.util.ResourceRequestHandlerUtil;
import com.bamboo.jdbc.PersistanceHelper;
import com.bamboo.rules.RuleExecutionEngine;
import com.bamboo.rules.util.RuleConstants;
import com.bamboo.core.util.HttpErrorHelperUtil;

@Component(value="ResourceRequestHandler")
@Path("/")
public class ResourceRequestHandler {
	
	@Autowired(required = true)
	private ResourceRegistry resourceRegistry;
	
	@Autowired(required = false)
	private MessageLocalizer messageLocalizer;
	
	@Autowired(required = true)
	private PersistanceHelper persistanceHelper;
	
	@Autowired(required = true)
	private Validator validator;
	
	@Autowired(required = true)
	private RuleExecutionEngine ruleExecutionEngine;
	
	@Autowired(required = true)
	private AuthenticationHandler authenticationHandler; 
	
	
	@Path("{resourceName}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response getFromID(@PathParam("resourceName") String resourceName, @PathParam("id") String id, @Context HttpServletRequest request, @Context HttpHeaders headers){
		Resource resource = resourceRegistry.getResource(resourceName);
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else
			try{
				/*if(!authorize(headers))
				return HttpErrorHelperUtil.getUnauthorizedResponse();*/
				
				Resource result = persistanceHelper.retrieveByID(id, resource);
				
				if(result == null)
					return HttpErrorHelperUtil.getUnknownIDResponse(id);
				
				ruleExecutionEngine.executeRules(result, resource.getResourceName(), resource.getResourceName()+RuleConstants.RULE_POST_GET);
				return HttpErrorHelperUtil.getSuccessResponse(result);
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
	}
	
	
	private Response bulkSearch(String resourceName, @Context HttpHeaders headers){
		Resource resource = resourceRegistry.getResource(resourceName);
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			try{
				/*if(!authorize(headers))
					return HttpErrorHelperUtil.getUnauthorizedResponse();*/
				ListResponse<Resource> listResponse = new ListResponse<>(persistanceHelper.retrieveAll(resource));
				ruleExecutionEngine.executeRules(listResponse, resource.getResourceName(), resource.getResourceName()+RuleConstants.RULE_POST_GET);
				return HttpErrorHelperUtil.getSuccessResponse(listResponse);
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
		}
	}
	
	
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response search(@PathParam("resourceName") String resourceName, @QueryParam("filter") String filter, @QueryParam("sortBy") String sortBy, @QueryParam("batchSize") int batchSize, @QueryParam("startIndex") int startIndex, @Context HttpServletRequest request, @Context HttpHeaders headers){
		if(filter == null && sortBy == null && batchSize == 0 && startIndex == 0){
			return bulkSearch(resourceName, headers);
		}
		Resource resource = resourceRegistry.getResource(resourceName);
		
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(resource.getSupportedFilters() == null){
				return HttpErrorHelperUtil.getBadRequestResponse(String.format(ResourceConstants.FILTERS_NOT_SUPPORTED, resourceName));
			}else{
				try{
					SearchCriteria searchCriteria = filter != null ? new FilterTranslatorUtil(resource.getSupportedFilters()).translateSearchQuery(null, filter.split(ResourceConstants.FILTER_SEPERATOR)) : null;
					SearchCriteria sortCriteria = sortBy != null ? FilterTranslatorUtil.translateSortQuery(sortBy) : null;
					return HttpErrorHelperUtil.getSuccessResponse(persistanceHelper.retrieveAllWithFilter(resource, searchCriteria, sortCriteria, batchSize, startIndex));
				}catch(Exception e){
					return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
				}
			}
		}
	}
	
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response save(@PathParam("resourceName") String resourceName, String postResource, @Context HttpServletRequest request){
		Resource resource = resourceRegistry.getResource(resourceName);
		
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(postResource == null || postResource.isEmpty())
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.BLANK_POST_REQUEST);
			
			JsonObject postObject = new JsonParser().parse(postResource).getAsJsonObject();
			Object parsedRootResource = new Gson().fromJson(postObject.get(ResourceConstants.POST_ROOT_TAG), resource.getResourceClass());
			
			if(parsedRootResource == null)
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.INCORRECT_REQUEST_BODY);
			List<String> messages = new ArrayList<>();
			messages = ResourceRequestHandlerUtil.validateResource((Resource)parsedRootResource, validator, true, messages, messageLocalizer, request.getLocale());
			
			if(!messages.isEmpty()){
				return HttpErrorHelperUtil.getBadRequestResponse(messages.toArray(new String[messages.size()]));
			}
			try{
				ruleExecutionEngine.executeRules(parsedRootResource, resource.getResourceName(), resource.getResourceName()+RuleConstants.RULE_PRE_CREATE);
				Response createResponse =  HttpErrorHelperUtil.getCreationSuccessResponse(persistanceHelper.save((Resource)parsedRootResource));
				ruleExecutionEngine.executeRules(parsedRootResource, resource.getResourceName(), resource.getResourceName()+RuleConstants.RULE_POST_CREATE);
				return createResponse;
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
		}
	}
	
	
	@Path("{resourceName}/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@DELETE
	public Response delete(@PathParam("resourceName") String resourceName, @PathParam("id") String id){
		Resource resource = resourceRegistry.getResource(resourceName);
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			try{
				//ruleExecutionEngine.executeRules(id, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_PRE_DELETE);
				persistanceHelper.delete(id, resource);
				return HttpErrorHelperUtil.getNoContentResponse();
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	@Path("{resourceName}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@PUT
	public Response update(@PathParam("resourceName") String resourceName, String jsonResource, @Context HttpServletRequest request, @PathParam("id") String id){
		Resource resource = resourceRegistry.getResource(resourceName);
		
		if(resource == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(jsonResource == null || jsonResource.isEmpty())
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.BLANK_POST_REQUEST);
			
			/*JsonObject postObject = new JsonParser().parse(jsonResource).getAsJsonObject();
			postObject = (JsonObject) postObject.get(ResourceConstants.POST_ROOT_TAG);
			
			if(resource == null)
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.INCORRECT_REQUEST_BODY);*/
			
			try{
				Resource originalResource = persistanceHelper.retrieveByID(id, resource);
				if(originalResource == null)
					return HttpErrorHelperUtil.getUnknownIDResponse(id);
				Map<String, Map<String, List<Object>>> operationsMap = ResourceRequestHandlerUtil.updateResourceFromRequest(originalResource, jsonResource);
				
				List<String> messages = new ArrayList<String>();
				messages = ResourceRequestHandlerUtil.validateResource(originalResource, validator, false, messages, messageLocalizer, request.getLocale());
				if(!messages.isEmpty()){
					return HttpErrorHelperUtil.getBadRequestResponse(messages.toArray(new String[messages.size()]));
				}
				
				//ruleExecutionEngine.executeRules(updatedResource, originalResource, null, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_PRE_UPDATE);
				originalResource = persistanceHelper.update(id, originalResource, operationsMap);
				//ruleExecutionEngine.executeRules(updatedResource, originalResource, updatedFields, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_POST_UPDATE);
				
				return HttpErrorHelperUtil.getSuccessResponse(originalResource);
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
		}
	}
	
	
	private boolean authorize( HttpHeaders headers){
		if(headers == null || headers.getHeaderString("Authorization") == null)
			return false;
		boolean isValid = false;
		try{
			String authToken = headers.getHeaderString("Authorization");
			isValid =  authenticationHandler.validateAuthenticationToken(authToken);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return isValid;
	}
	
	private String getLinkedResourceName(JsonElement jsonElement){
		return jsonElement.getAsJsonObject().entrySet().stream().findFirst().get().getKey();
	}
	
}