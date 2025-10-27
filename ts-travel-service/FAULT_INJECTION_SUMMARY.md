# Fault Injection Summary - ts-travel-service

## Injected Faults

### 1. SAME_STATION_FAULT
- **Endpoint:** `POST /api/v1/travelservice/trips/left`
- **Triggers:** startPlace equals endPlace (case-insensitive)
- **Test:** Send request with `startPlace: "Beijing"` and `endPlace: "beijing"`

### 2. PAST_DATE_FAULT
- **Endpoint:** `POST /api/v1/travelservice/trips/left`
- **Triggers:** departureTime is a date in the past
- **Test:** Send request with `departureTime: "2020-01-01"`

### 3. INVALID_DATE_FORMAT_FAULT
- **Endpoint:** `POST /api/v1/travelservice/trips/left`
- **Triggers:** departureTime is not in format yyyy-MM-dd
- **Test:** Send request with `departureTime: "01/15/2025"` or `"2025-15-01"`

## Files Modified
- `src/main/java/travel/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/travel/controller/TravelController.java` (MODIFIED)

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

