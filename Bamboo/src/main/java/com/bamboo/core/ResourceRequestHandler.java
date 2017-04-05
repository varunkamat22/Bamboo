package com.bamboo.core;

import java.util.ArrayList;
import java.util.List;
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
	
	@SuppressWarnings("rawtypes")
	@Path("{resourceName}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response getFromID(@PathParam("resourceName") String resourceName, @PathParam("id") String id, @Context HttpServletRequest request, @Context HttpHeaders headers){
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else
			try{
				if(!authorize(headers))
					return HttpErrorHelperUtil.getUnauthorizedResponse();
				Object result = persistanceHelper.retrieveByID(id, resourceName, resourceManager.getResourceClass());
				if(result == null)
					return HttpErrorHelperUtil.getUnknownIDResponse(id);
				else
					ruleExecutionEngine.executeRules(result, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_POST_GET);
					return HttpErrorHelperUtil.getSuccessResponse(result);
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
	}
	

	@SuppressWarnings("unchecked")
	private Response bulkSearch(String resourceName, @Context HttpHeaders headers){
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			try{
				if(!authorize(headers))
					return HttpErrorHelperUtil.getUnauthorizedResponse();
				ListResponse listResponse = new ListResponse(persistanceHelper.retrieveAll(resourceManager.getResourceName(), resourceManager.getResourceClass()));
				ruleExecutionEngine.executeRules(listResponse, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_POST_GET);
				return HttpErrorHelperUtil.getSuccessResponse(listResponse);
			}catch(Exception e){
				return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
			}
		}
	}
	

	@SuppressWarnings("rawtypes")
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response search(@PathParam("resourceName") String resourceName, @QueryParam("filter") String filter, @QueryParam("sortBy") String sortBy, @QueryParam("batchSize") int batchSize, @QueryParam("startIndex") int startIndex, @Context HttpServletRequest request, @Context HttpHeaders headers){
		if(filter == null && sortBy == null && batchSize == 0 && startIndex == 0){
			return bulkSearch(resourceName, headers);
		}
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(resourceManager.getSupportedFilters() == null){
				return HttpErrorHelperUtil.getBadRequestResponse(String.format(ResourceConstants.FILTERS_NOT_SUPPORTED, resourceName));
			}else{
				try{
					SearchCriteria searchCriteria = filter != null ? new FilterTranslatorUtil(resourceManager.getSupportedFilters()).translateSearchQuery(null, filter.split(ResourceConstants.FILTER_SEPERATOR)) : null;
					SearchCriteria sortCriteria = sortBy != null ? FilterTranslatorUtil.translateSortQuery(sortBy) : null;
					return HttpErrorHelperUtil.getSuccessResponse(persistanceHelper.retrieveAllWithFilter(resourceName, resourceManager.getResourceClass(), searchCriteria, sortCriteria, batchSize, startIndex));//resourceManager.get(searchCriteria, sortCriteria, batchSize, startIndex));
				}catch(Exception e){
					return HttpErrorHelperUtil.getServerErrorResponse(e.getMessage());
				}
			}
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response save(@PathParam("resourceName") String resourceName, String postResource, @Context HttpServletRequest request){
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(postResource == null || postResource.isEmpty())
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.BLANK_POST_REQUEST);
			
			JsonObject postObject = new JsonParser().parse(postResource).getAsJsonObject();
			Object resource = new Gson().fromJson(postObject.get(ResourceConstants.POST_ROOT_TAG), resourceManager.getResourceClass());
			
			if(resource == null)
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.INCORRECT_REQUEST_BODY);
			List<String> messages = ResourceRequestHandlerUtil.validateResource(resource, validator, messageLocalizer, request.getLocale());
			if(!messages.isEmpty()){
				return HttpErrorHelperUtil.getBadRequestResponse(messages.toArray(new String[messages.size()]));
			}
			try{
				ruleExecutionEngine.executeRules(resource, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_PRE_CREATE);
				Response createResponse =  HttpErrorHelperUtil.getCreationSuccessResponse(persistanceHelper.save(resource, resourceName, resourceManager.getResourceClass()));
				ruleExecutionEngine.executeRules(resource, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_POST_CREATE);
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
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			try{
				ruleExecutionEngine.executeRules(id, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_PRE_DELETE);
				persistanceHelper.delete(id, resourceName);
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
	public Response update(@PathParam("resourceName") String resourceName, String postResource, @Context HttpServletRequest request, @PathParam("id") String id){
		ResourceManager resourceManager = resourceRegistry.getResourceManager(resourceName);
		if(resourceManager == null)
			return HttpErrorHelperUtil.getResourceNotFoundResponse(resourceName);
		else{
			if(postResource == null || postResource.isEmpty())
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.BLANK_POST_REQUEST);
			
			JsonObject postObject = new JsonParser().parse(postResource).getAsJsonObject();
			JsonObject resource = (JsonObject) postObject.get(ResourceConstants.POST_ROOT_TAG);
			if(resource == null)
				return HttpErrorHelperUtil.getBadRequestResponse(ResourceConstants.INCORRECT_REQUEST_BODY);
			
			try{
				Object originalResource = persistanceHelper.retrieveByID(id, resourceName, resourceManager.getResourceClass());
				if(originalResource == null)
					return HttpErrorHelperUtil.getUnknownIDResponse(id);
				Object updatedResource = ResourceRequestHandlerUtil.updateResourceFromRequest(originalResource, resource);
				
				List<String> messages = ResourceRequestHandlerUtil.validateResource(updatedResource, validator, messageLocalizer, request.getLocale());
				if(!messages.isEmpty()){
					return HttpErrorHelperUtil.getBadRequestResponse(messages.toArray(new String[messages.size()]));
				}
				
				List<String> updatedFields = new ArrayList<String>();
				resource.entrySet().forEach(entry -> {
					updatedFields.add(entry.getKey());
				});
				ruleExecutionEngine.executeRules(updatedResource, originalResource, updatedFields, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_PRE_UPDATE);
				updatedResource = persistanceHelper.update(id, resourceName, resourceManager.getResourceClass(), updatedResource);
				ruleExecutionEngine.executeRules(updatedResource, originalResource, updatedFields, resourceManager.getResourceName(), resourceManager.getResourceName()+RuleConstants.RULE_POST_UPDATE);
				
				return HttpErrorHelperUtil.getSuccessResponse(updatedResource);
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
	
}