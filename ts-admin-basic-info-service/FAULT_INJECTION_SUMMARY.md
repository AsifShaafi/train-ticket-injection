# Fault Injection Summary - ts-admin-basic-info-service

## Injected Faults

### 1. INVALID_PRICE_RATE_FAULT
- **Endpoint:** `POST /api/v1/adminbasicservice/adminbasic/prices`
- **Triggers:** basicPriceRate or firstClassPriceRate is less than or equal to 0
- **Test:** Send price with `basicPriceRate: -1.0` or `firstClassPriceRate: 0.0`

### 2. INVALID_ROUTE_ID_FAULT
- **Endpoint:** `POST /api/v1/adminbasicservice/adminbasic/prices`
- **Triggers:** routeId is null or empty
- **Test:** Send price with `routeId: null` or `routeId: ""`

## Files Modified
- `src/main/java/adminbasic/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/adminbasic/service/AdminBasicInfoServiceImpl.java` (MODIFIED)

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

