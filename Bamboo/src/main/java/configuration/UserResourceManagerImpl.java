package configuration;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import com.bamboo.core.ResourceManager;
import com.bamboo.core.SearchCriteria;

@Component
public class UserResourceManagerImpl implements ResourceManager<String> {

	@Override
	public String getFromId(String id) {
		return "Jhon";
	}

	@Override
	public List<String> get(SearchCriteria sc) {
		return Arrays.asList("Jhon", "James");
	}

	@Override
	public String save(String t) {		
		return "aababab11234";
	}

	@Override
	public String update(String t) {		
		return null;
	}

	@Override
	public void delete(String id) {		
		return;
	}

	@Override
	public String getResourceName() {
		return "User";
	}

}
