package resourceManagerTests;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import com.bamboo.core.ResourceRegistry;

public class ResourceRegistryTest {
	
	//Should refactor this code to accept a different path for base package
	@Test
	public void testResourceRegistry(){
		ApplicationContext context = new AnnotationConfigApplicationContext(TestDIConfig.class);
		ResourceRegistry routeRegistry = context.getBean(com.bamboo.core.ResourceRegistry.class);
		assertNotNull(routeRegistry.getResourceManager("User"));
	}

}
