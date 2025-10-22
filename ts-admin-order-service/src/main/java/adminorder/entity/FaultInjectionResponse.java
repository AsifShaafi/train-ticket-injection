package adminorder.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper for injected faults to distinguish them from actual system errors
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaultInjectionResponse {
    private boolean isInjected;
    private String faultName;
    private String message;
    private Object details;

    public FaultInjectionResponse(boolean isInjected, String faultName, String message) {
        this.isInjected = isInjected;
        this.faultName = faultName;
        this.message = message;
        this.details = null;
    }
}

