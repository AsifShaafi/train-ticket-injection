# Fault Injection Summary - ts-admin-route-service

## Injected Faults

### 1. INSUFFICIENT_STATIONS_FAULT
- **Endpoint:** `POST /api/v1/adminrouteservice/adminroute`
- **Triggers:** station list is null, empty, or contains less than 2 stations
- **Test:** Send route with `stationList: "Shanghai"` (only 1 station) or `stationList: ""`

### 2. INVALID_STATION_NAME_LENGTH_FAULT
- **Endpoint:** `POST /api/v1/adminrouteservice/adminroute`
- **Triggers:** individual station name length < 2 or > 50 characters
- **Test:** Send route with station "A" (1 char) or "AAA...AAA" (51+ chars) in the list

## Files Modified
- `src/main/java/adminroute/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/adminroute/service/AdminRouteServiceImpl.java` (MODIFIED)

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

