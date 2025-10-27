# Fault Injection Summary - ts-travel-plan-service

## Injected Faults

### 1. INVALID_STATION_NAME_FAULT
- **Endpoint:** `POST /api/v1/travelplanservice/travelPlan/minStation`
- **Triggers:** startPlace or endPlace is null or empty
- **Test:** Send request with `startPlace: null` or `endPlace: ""`

### 2. SAME_STATION_FAULT
- **Endpoint:** `POST /api/v1/travelplanservice/travelPlan/minStation`
- **Triggers:** startPlace equals endPlace (case-insensitive)
- **Test:** Send request with `startPlace: "Shanghai"` and `endPlace: "shanghai"`

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

