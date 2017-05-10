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
public final class ResourceRegistry {

	private Map<String, Resource> routeRegistry;
	
	@Autowired(required=false)
	private List<Resource> resources;
	
	@PostConstruct
	private void buildRegistry(){
		System.out.println("Total number of resources found>>"+resources.size());
		
		if(resources != null && !resources.isEmpty()){
			routeRegistry = new HashMap<>();
			resources.forEach(res -> {
				if(routeRegistry.get(res.getResourceName()) != null)
						throw new RuntimeException("Duplicate resource found - "+res.getResourceName());
				routeRegistry.put(res.getResourceName(), res);
			});
		}
		resources= null; //release for GC
	}
	
	public Resource getResource(String resourceName){
		return routeRegistry.get(resourceName);
	}
}
