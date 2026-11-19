package cm.alerte.disparition.config;

import cm.alerte.disparition.stockage.StockageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    private final StockageService stockage;

    public WebConfig(StockageService stockage) {
        this.stockage = stockage;
    }

    @Bean
    Clock horloge() {
        return Clock.systemDefaultZone();
    }

    /** Les photos ont un nom aleatoire et ne changent jamais : cache long. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(stockage.racine().toUri().toString())
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable());
    }
}
