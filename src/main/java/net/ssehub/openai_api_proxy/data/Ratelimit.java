package net.ssehub.openai_api_proxy.data;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Ratelimit {

    private static final Logger LOG = Logger.getLogger(Ratelimit.class.getName());
    
    @JsonIgnore
    @Id
    private String type;
    
    private Integer maximum;
    
    private Integer remaining;
    
    private ZonedDateTime reset;
    
    public Ratelimit(String type, String headerLimit, String headerRemaining, String headerReset) {
        this.type = type;
        this.maximum = parseOrNull(headerLimit);
        this.remaining = parseOrNull(headerRemaining);
        this.reset = parseResetTime(headerReset);
    }

    // for JPA
    protected Ratelimit() {
    }
    
    public String getType() {
        return type;
    }
    
    public Integer getMaximum() {
        return maximum;
    }
    
    public Integer getRemaining() {
        return remaining;
    }
    
    public ZonedDateTime getReset() {
        return reset;
    }
    
    private static ZonedDateTime parseResetTime(String reset) {
        ZonedDateTime result = null;
        if (reset != null) {
            if (reset.matches("\\d+ms")) {
                reset = "0." + reset.substring(0, reset.length() - 2) + "s";
            }
            reset = "PT" + reset;
            
            try {
                result = ZonedDateTime.now().plus(Duration.parse(reset));
            } catch (DateTimeParseException e) {
                LOG.log(Level.WARNING, "Failed to reset duration" , e);
            }
        }
        
        return result;
    }
    
    private static Integer parseOrNull(String string) {
        Integer result = null;
        if (string != null) {
            try {
                result = Integer.parseInt(string);
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Failed to parse integer" , e);
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Ratelimit for " + type + ": " + remaining + " remaining of " + maximum + ", reset at " + reset;
    }
    
}
