package com.bamboo.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("resourceRegistry")
@Scope("singleton")
@SuppressWarnings("rawtypes")
public final class ResourceRegistry {

	private Map<String, ResourceManager> routeRegistry;
	
	@Autowired(required=false) 
	private List<ResourceManager> resourceManagers;
	
	@PostConstruct
	private void buildRegistry(){
		System.out.println("Total number of resources found>>"+resourceManagers.size());
		
		if(resourceManagers != null && !resourceManagers.isEmpty()){
			routeRegistry = new HashMap<>();
			resourceManagers.forEach(rm -> {
				if(routeRegistry.get(rm.getResourceName()) != null)
						throw new RuntimeException("Duplicate resource found - "+rm.getResourceName());
				routeRegistry.put(rm.getResourceName(), rm);
			});
		}
		resourceManagers = null; //release for GC
	}
	
	public ResourceManager getResourceManager(String resourceName){
		return routeRegistry.get(resourceName);
	}
}
