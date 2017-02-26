package resourceManagerTests;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import com.bamboo.core.DIConfig;

@Configuration
@ComponentScan(basePackages = {"resourceManagerTests","com.bamboo.core"}, excludeFilters = {
        		@Filter(type = FilterType.ANNOTATION, value={Configuration.class})
    })
public class TestDIConfig{

}
