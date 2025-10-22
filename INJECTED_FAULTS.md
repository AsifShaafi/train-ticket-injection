# Injected Faults Documentation

This document records all injected faults across microservices for black-box testing validation.

---

## ts-admin-order-service

### 1. INVALID_CONTACTS_NAME_FAULT

**API:** `POST /api/v1/adminorderservice/adminorder`

**Description:** Rejects order creation when `contactsName` is null, empty, or purely numeric.

**Trigger Conditions:**
- `contactsName` is null or empty string
- `contactsName` contains only digits (e.g., "12345")

**Sample Input:**
```json
{
  "accountId": "user123",
  "contactsName": "12345",
  "trainNumber": "G1234",
  "seatNumber": "5A",
  "from": "Shanghai",
  "to": "Beijing"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Order rejected: contactsName cannot be purely numeric",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_CONTACTS_NAME_FAULT",
    "message": "Order rejected: contactsName cannot be purely numeric",
    "details": "12345"
  }
}
```

---

### 2. INVALID_SEAT_NUMBER_FAULT

**API:** `PUT /api/v1/adminorderservice/adminorder`

**Description:** Rejects order updates when `seatNumber` doesn't follow the required format (digits followed by uppercase letter).

**Trigger Conditions:**
- `seatNumber` is null or empty
- `seatNumber` doesn't match pattern `^\d+[A-Z]$` (e.g., "5a", "A5", "5", "AB")

**Sample Input:**
```json
{
  "id": "order-123-456-789",
  "accountId": "user123",
  "contactsName": "John Doe",
  "trainNumber": "G1234",
  "seatNumber": "5a",
  "from": "Shanghai",
  "to": "Beijing"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Order update rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_SEAT_NUMBER_FAULT",
    "message": "Order update rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)",
    "details": "5a"
  }
}
```

---

### 3. INVALID_ORDER_ID_LENGTH_FAULT

**API:** `DELETE /api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}`

**Description:** Rejects order deletion when `orderId` length is outside the valid range (10-50 characters).

**Trigger Conditions:**
- `orderId` is null or empty
- `orderId` length < 10 characters
- `orderId` length > 50 characters

**Sample Input:**
```
DELETE /api/v1/adminorderservice/adminorder/short123/G1234
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Order deletion rejected: orderId length must be between 10 and 50 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_ORDER_ID_LENGTH_FAULT",
    "message": "Order deletion rejected: orderId length must be between 10 and 50 characters",
    "details": "OrderID: 'short123', Length: 8"
  }
}
```

---

## Detection Guidelines

All injected faults can be identified by:
1. **Response status:** `0` (failure)
2. **Response data contains:** `"isInjected": true`
3. **Response data contains:** Specific `faultName` field

These characteristics distinguish injected faults from genuine system errors, enabling black-box testing tools to accurately detect and report them.

