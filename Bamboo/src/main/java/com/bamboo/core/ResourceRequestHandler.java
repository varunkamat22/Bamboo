package com.bamboo.core;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component
@Path("/")
public class ResourceRequestHandler {
	
	@Autowired
	private ResourceRegistry registry;
	
	/**
	 * This should handle get by ID
	 */
	@SuppressWarnings("rawtypes")
	@Path("{resourceName}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response get(@PathParam("resourceName") String resourceName, @PathParam("id") String id){
		ResourceManager resourceManager = registry.getResourceManager(resourceName);
		Gson gson = new Gson();
		if(resourceManager == null)
			return Response.status(404).entity(gson.toJson(HttpError.get404Error("Resource not found"))).build();
		else{
			return Response.status(200).entity(gson.toJson(resourceManager.getFromId(id))).build();
		}
	}
	

	private Response bulkSearch(String resourceName){
		ResourceManager resourceManager = registry.getResourceManager(resourceName);
		Gson gson = new Gson();
		if(resourceManager == null)
			return Response.status(404).entity(gson.toJson(HttpError.get404Error("Resource not found"))).build();
		else{
			ListResponse listResponse = new ListResponse(resourceManager.get(null));
			return Response.status(200).entity(gson.toJson(listResponse)).build();
		}
	}
	
	/**
	 * This should handle get by ID
	 */
	@SuppressWarnings("rawtypes")
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response search(@PathParam("resourceName") String resourceName, @QueryParam("filter") String filter){
		if(filter == null){
			return bulkSearch(resourceName);
		}
		ResourceManager resourceManager = registry.getResourceManager(resourceName);
		
		Gson gson = new Gson();
		if(resourceManager == null)
			return Response.status(404).entity(gson.toJson(HttpError.get404Error("Resource not found"))).build();
		else{
			if(resourceManager.getSupportedFilters() == null)
				return Response.status(404).entity(gson.toJson(HttpError.get404Error("Filters are not supported for this resource"))).build();
			SearchCriteria searchCriteria = new FilterTranslatorUtil(resourceManager.getSupportedFilters()).translate(null, filter.split(" "));
			return Response.status(200).entity(gson.toJson(resourceManager.get(searchCriteria))).build();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Path("{resourceName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response save(@PathParam("resourceName") String resourceName, String jsonResource){
		ResourceManager resourceManager = registry.getResourceManager(resourceName);
		Gson gson = new Gson();
		if(resourceManager == null)
			return Response.status(404).entity(gson.toJson(HttpError.get404Error("Resource not found"))).build();
		else{
			JsonObject jsonObject = new JsonParser().parse(jsonResource).getAsJsonObject();
			Object resource = gson.fromJson(jsonObject.get("resource"), resourceManager.getResourceClass());
			return Response.status(200).entity(gson.toJson(resourceManager.save(resourceManager.getResourceClass().cast(resource)))).build();
		}
	}
	
	@Path("hello")
	@GET
	public String test(){
		return "Hello World";
	}
}
