# Fault Injection Summary - ts-admin-travel-service

## Injected Faults

### 1. INVALID_TRIP_ID_FORMAT_FAULT
- **Endpoint:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`
- **Triggers:** tripId is null or empty
- **Test:** DELETE with `tripId: null` or `tripId: ""`

### 2. INVALID_TRIP_ID_LENGTH_FAULT
- **Endpoint:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`
- **Triggers:** tripId length is less than 4 or greater than 20 characters
- **Test:** DELETE with `tripId: "G12"` (too short) or `tripId: "G123456789012345678901"` (too long)

## Files Modified
- `src/main/java/admintravel/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/admintravel/service/AdminTravelServiceImpl.java` (MODIFIED)

## Response Format
All injected faults return:
```json
{
  "status": 0,
  "msg": "[error description]",
  "data": {
    "isInjected": true,
    "faultName": "[FAULT_NAME]",
    "message": "[detailed message]",
    "details": "[optional context]"
  }
}
```

