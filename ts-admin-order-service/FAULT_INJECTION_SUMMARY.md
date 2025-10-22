# Fault Injection Summary - ts-admin-order-service

## Injected Faults

### 1. INVALID_CONTACTS_NAME_FAULT
- **Endpoint:** `POST /api/v1/adminorderservice/adminorder`
- **Triggers:** contactsName is null, empty, or purely numeric
- **Test:** Send order with `contactsName: "12345"`

### 2. INVALID_SEAT_NUMBER_FAULT
- **Endpoint:** `PUT /api/v1/adminorderservice/adminorder`
- **Triggers:** seatNumber doesn't match pattern `^\d+[A-Z]$`
- **Test:** Send update with `seatNumber: "5a"` or `"A5"`

### 3. INVALID_ORDER_ID_LENGTH_FAULT
- **Endpoint:** `DELETE /api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}`
- **Triggers:** orderId length < 10 or > 50 characters
- **Test:** Delete with `orderId: "short123"` (8 chars)

## Files Modified
- `src/main/java/adminorder/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/adminorder/service/AdminOrderServiceImpl.java` (MODIFIED)

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


