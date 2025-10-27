package travel.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response entity for injected faults
 */
@Data
@NoArgsConstructor
public class FaultInjectionResponse {
    private boolean isInjected;
    private String faultName;
    private String message;
    private String details;

    public FaultInjectionResponse(boolean isInjected, String faultName, String message) {
        this.isInjected = isInjected;
        this.faultName = faultName;
        this.message = message;
    }

    public FaultInjectionResponse(boolean isInjected, String faultName, String message, String details) {
        this.isInjected = isInjected;
        this.faultName = faultName;
        this.message = message;
        this.details = details;
    }
}

