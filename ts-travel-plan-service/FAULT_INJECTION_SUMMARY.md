# Fault Injection Summary - ts-travel-plan-service

## Injected Faults

### 1. INVALID_STATION_NAME_FAULT
- **Endpoint:** `POST /api/v1/travelplanservice/travelPlan/minStation`
- **Triggers:** startPlace or endPlace is null or empty
- **Test:** Send request with `startPlace: null` or `endPlace: ""`

### 2. INVALID_STATION_LENGTH_FAULT
- **Endpoint:** `POST /api/v1/travelplanservice/travelPlan/minStation`
- **Triggers:** startPlace or endPlace length < 2 or > 50 characters
- **Test:** Send request with `startPlace: "A"` (1 char) or `startPlace: "A...A"` (51+ chars)

## Files Modified
- `src/main/java/travelplan/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/travelplan/service/TravelPlanServiceImpl.java` (MODIFIED)

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

