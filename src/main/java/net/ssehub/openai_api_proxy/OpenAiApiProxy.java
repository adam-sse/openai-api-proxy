package net.ssehub.openai_api_proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.EncodingRegistry;

@SpringBootApplication
@Configuration
public class OpenAiApiProxy {
    
    public static void main(String[] args) {
        SpringApplication.run(OpenAiApiProxy.class, args);
    }
    
    @Bean
    public EncodingRegistry encodingRegistry() {
        return Encodings.newDefaultEncodingRegistry();
    }

}
