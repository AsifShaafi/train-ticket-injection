# Injected Faults Documentation

This document records all injected faults across microservices for black-box testing validation.

---

## ts-admin-order-service

### 1. INVALID_CONTACTS_NAME_FAULT

**API:** `POST /api/v1/adminorderservice/adminorder` AND `PUT /api/v1/adminorderservice/adminorder`

**Description:** Rejects order creation/updates when `contactsName` is null, empty, or purely numeric.

**Trigger Conditions:**
- `contactsName` is null, empty, or contains only whitespace
- `contactsName` contains only digits after trimming whitespace (e.g., "12345", " 999 ")

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
- **HTTP Status Code:** 400 Bad Request
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

**API:** `POST /api/v1/adminorderservice/adminorder` AND `PUT /api/v1/adminorderservice/adminorder`

**Description:** Rejects order creation/updates when `seatNumber` doesn't follow the required format (digits followed by uppercase letter).

**Trigger Conditions:**
- `seatNumber` is null, empty, or contains only whitespace
- `seatNumber` doesn't match pattern `^\d+[A-Z]$` after trimming (e.g., "5a", "A5", "5", "AB", " ")

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

## ts-admin-basic-info-service

### 3. INVALID_PRICE_RATE_FAULT

**API:** `POST /api/v1/adminbasicservice/adminbasic/prices`

**Description:** Rejects price creation when price rates are non-positive.

**Trigger Conditions:**
- `basicPriceRate` ≤ 0
- `firstClassPriceRate` ≤ 0

**Sample Input:**
```json
{
  "trainType": "G",
  "routeId": "route-123",
  "basicPriceRate": -1.0,
  "firstClassPriceRate": 2.5
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Price creation rejected: basicPriceRate must be positive",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_PRICE_RATE_FAULT",
    "message": "Price creation rejected: basicPriceRate must be positive",
    "details": "basicPriceRate: -1.00"
  }
}
```

---

### 4. INVALID_ROUTE_ID_FAULT

**API:** `POST /api/v1/adminbasicservice/adminbasic/prices`

**Description:** Rejects price creation when routeId is null or empty.

**Trigger Conditions:**
- `routeId` is null, empty, or contains only whitespace

**Sample Input:**
```json
{
  "trainType": "G",
  "routeId": "",
  "basicPriceRate": 1.0,
  "firstClassPriceRate": 2.5
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Price creation rejected: routeId cannot be null or empty",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_ROUTE_ID_FAULT",
    "message": "Price creation rejected: routeId cannot be null or empty"
  }
}
```

---

## ts-travel-plan-service

### 5. INVALID_STATION_NAME_FAULT

**API:** `POST /api/v1/travelplanservice/travelPlan/minStation`

**Description:** Rejects travel plan request when station names are null or empty.

**Trigger Conditions:**
- `startPlace` is null, empty, or contains only whitespace
- `endPlace` is null, empty, or contains only whitespace

**Sample Input:**
```json
{
  "startPlace": "",
  "endPlace": "Beijing",
  "departureTime": "2025-11-01"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Travel plan request rejected: startPlace cannot be null or empty",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_STATION_NAME_FAULT",
    "message": "Travel plan request rejected: startPlace cannot be null or empty"
  }
}
```

---

### 6. INVALID_STATION_LENGTH_FAULT (Travel Plan)

**API:** `POST /api/v1/travelplanservice/travelPlan/minStation`

**Description:** Rejects travel plan when station name length is outside valid range.

**Trigger Conditions:**
- `startPlace` length < 2 or > 50 characters after trimming
- `endPlace` length < 2 or > 50 characters after trimming

**Sample Input:**
```json
{
  "startPlace": "A",
  "endPlace": "Beijing",
  "departureTime": "2025-11-01"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Travel plan request rejected: startPlace length must be between 2 and 50 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_STATION_LENGTH_FAULT",
    "message": "Travel plan request rejected: startPlace length must be between 2 and 50 characters",
    "details": "startPlace: 'A', Length: 1"
  }
}
```

---

## ts-admin-travel-service

### 7. INVALID_TRIP_ID_FORMAT_FAULT

**API:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`

**Description:** Rejects trip deletion when tripId format is invalid.

**Trigger Conditions:**
- `tripId` is null, empty, or contains only whitespace

**Sample Input:**
```
DELETE /api/v1/admintravelservice/admintravel/
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Trip deletion rejected: tripId cannot be null or empty",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_TRIP_ID_FORMAT_FAULT",
    "message": "Trip deletion rejected: tripId cannot be null or empty"
  }
}
```

---

### 8. INVALID_TRIP_ID_LENGTH_FAULT

**API:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`

**Description:** Rejects trip deletion when tripId length is invalid.

**Trigger Conditions:**
- `tripId` length < 4 or > 20 characters after trimming

**Sample Input:**
```
DELETE /api/v1/admintravelservice/admintravel/G12
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Trip deletion rejected: tripId length must be between 4 and 20 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_TRIP_ID_LENGTH_FAULT",
    "message": "Trip deletion rejected: tripId length must be between 4 and 20 characters",
    "details": "TripID: 'G12', Length: 3"
  }
}
```

---

## ts-admin-route-service

### 9. INSUFFICIENT_STATIONS_FAULT

**API:** `POST /api/v1/adminrouteservice/adminroute`

**Description:** Rejects route creation when station list is insufficient.

**Trigger Conditions:**
- `stationList` is null or empty
- Number of stations < 2 (individual station names are not trimmed, but empty entries are still invalid)

**Sample Input:**
```json
{
  "id": "route-123",
  "startStation": "Shanghai",
  "endStation": "Shanghai",
  "stationList": "Shanghai"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Route creation rejected: route must have at least 2 stations",
  "data": {
    "isInjected": true,
    "faultName": "INSUFFICIENT_STATIONS_FAULT",
    "message": "Route creation rejected: route must have at least 2 stations",
    "details": "Number of stations: 1"
  }
}
```

---

### 10. INVALID_STATION_NAME_LENGTH_FAULT

**API:** `POST /api/v1/adminrouteservice/adminroute`

**Description:** Rejects route creation when individual station name length is outside valid range.

**Trigger Conditions:**
- Any station name in `stationList` is null, empty, or contains only whitespace
- Any station name length < 2 or > 50 characters after trimming

**Sample Input:**
```json
{
  "id": "route-123",
  "startStation": "Shanghai",
  "endStation": "Beijing",
  "stationList": "Shanghai,X,Beijing"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Route creation rejected: each station name must be between 2 and 50 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_STATION_NAME_LENGTH_FAULT",
    "message": "Route creation rejected: each station name must be between 2 and 50 characters",
    "details": "Station: 'X', Length: 1"
  }
}
```

---

## Detection Guidelines

All injected faults can be identified by:
1. **HTTP Status Code:** `400 Bad Request` (instead of 200)
2. **Response status:** `0` (failure) in the response body
3. **Response data contains:** `"isInjected": true`
4. **Response data contains:** Specific `faultName` field

**Important:** When an injected fault is triggered, the API returns:
- HTTP 400 status code (not HTTP 200)
- Application-level status of 0 in the response body
- The `data` field contains fault injection details with `"isInjected": true`

These characteristics distinguish injected faults from genuine system errors, enabling black-box testing tools to accurately detect and report them.

## Summary

**Total Services Injected:** 5
- ts-admin-order-service
- ts-admin-basic-info-service
- ts-travel-plan-service
- ts-admin-travel-service
- ts-admin-route-service

**Total Fault Types:** 10

**Injected APIs:**
1. POST /api/v1/adminorderservice/adminorder (validates contactsName and seatNumber)
2. PUT /api/v1/adminorderservice/adminorder (validates contactsName and seatNumber)
3. POST /api/v1/adminbasicservice/adminbasic/prices
4. POST /api/v1/travelplanservice/travelPlan/minStation
5. DELETE /api/v1/admintravelservice/admintravel/{tripId}
6. POST /api/v1/adminrouteservice/adminroute

