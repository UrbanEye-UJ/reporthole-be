package za.co.urbaneye.reporthole.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "services.web")
@Getter
@Setter
public class WebProperties {
    private List<String> allowed;
}