# Fault Injection Summary - ts-admin-route-service

## Injected Faults

### 1. INSUFFICIENT_STATIONS_FAULT
- **Endpoint:** `POST /api/v1/adminrouteservice/adminroute`
- **Triggers:** station list is null, empty, or contains less than 2 stations
- **Test:** Send route with `stationList: "Shanghai"` (only 1 station) or `stationList: ""`

### 2. DUPLICATE_STATIONS_FAULT
- **Endpoint:** `POST /api/v1/adminrouteservice/adminroute`
- **Triggers:** station list contains duplicate stations (case-insensitive)
- **Test:** Send route with `stationList: "Shanghai,Beijing,shanghai"`

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

