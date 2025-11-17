# Fault Injection Summary - ts-travel-service

## Injected Faults

### 1. INVALID_STATION_LENGTH_FAULT
- **Endpoint:** `POST /api/v1/travelservice/trips/left`
- **Triggers:** startPlace or endPlace length < 2 or > 50 characters
- **Test:** Send request with `startPlace: "A"` (1 char) or `startPlace: "A...A"` (51+ chars)

### 2. INVALID_YEAR_RANGE_FAULT
- **Endpoint:** `POST /api/v1/travelservice/trips/left`
- **Triggers:** departureTime year < 2000 or > 2100
- **Test:** Send request with `departureTime: "1999-01-01"` or `departureTime: "2101-01-01"`

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

