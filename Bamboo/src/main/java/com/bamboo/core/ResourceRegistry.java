package com.bamboo.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class ResourceRegistry {
	
	@Autowired(required=false)
	private List<ResourceManager> resourceManagers;
	
	private Map<String, ResourceManager> routeRegistry;
	
	@PostConstruct
	private void buildRegistry(){
		System.out.println("Total number of resources found>>"+resourceManagers.size());
		
		routeRegistry = new HashMap<>();
		resourceManagers.forEach(element -> {
			if(routeRegistry.get(element.getResourceName()) != null)
					throw new RuntimeException("Duplicate resource found - "+element.getResourceName());
			routeRegistry.put(element.getResourceName(), element);
		});
		resourceManagers = null; //releases the list for GC
	}
	
	public ResourceManager getResourceManager(String resourceName){
		return routeRegistry.get(resourceName);
	}
}
