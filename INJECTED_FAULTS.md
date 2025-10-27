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

---

## ts-admin-basic-info-service

### 4. INVALID_PRICE_RATE_FAULT

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

### 5. INVALID_ROUTE_ID_FAULT

**API:** `POST /api/v1/adminbasicservice/adminbasic/prices`

**Description:** Rejects price creation when routeId is null or empty.

**Trigger Conditions:**
- `routeId` is null or empty string

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

### 6. INVALID_STATION_NAME_FAULT

**API:** `POST /api/v1/travelplanservice/travelPlan/minStation`

**Description:** Rejects travel plan request when station names are null or empty.

**Trigger Conditions:**
- `startPlace` is null or empty
- `endPlace` is null or empty

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

### 7. SAME_STATION_FAULT (Travel Plan)

**API:** `POST /api/v1/travelplanservice/travelPlan/minStation`

**Description:** Rejects travel plan when start and end stations are the same.

**Trigger Conditions:**
- `startPlace` equals `endPlace` (case-insensitive)

**Sample Input:**
```json
{
  "startPlace": "Shanghai",
  "endPlace": "shanghai",
  "departureTime": "2025-11-01"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Travel plan request rejected: startPlace and endPlace cannot be the same",
  "data": {
    "isInjected": true,
    "faultName": "SAME_STATION_FAULT",
    "message": "Travel plan request rejected: startPlace and endPlace cannot be the same",
    "details": "Station: Shanghai"
  }
}
```

---

## ts-travel-service

### 8. SAME_STATION_FAULT (Trip Query)

**API:** `POST /api/v1/travelservice/trips/left`

**Description:** Rejects trip query when start and end stations are the same.

**Trigger Conditions:**
- `startPlace` equals `endPlace` (case-insensitive)

**Sample Input:**
```json
{
  "startPlace": "Beijing",
  "endPlace": "beijing",
  "departureTime": "2025-11-01"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Trip query rejected: startPlace and endPlace cannot be the same",
  "data": {
    "isInjected": true,
    "faultName": "SAME_STATION_FAULT",
    "message": "Trip query rejected: startPlace and endPlace cannot be the same",
    "details": "Station: Beijing"
  }
}
```

---

### 9. PAST_DATE_FAULT

**API:** `POST /api/v1/travelservice/trips/left`

**Description:** Rejects trip query when departure date is in the past.

**Trigger Conditions:**
- `departureTime` is a date before today

**Sample Input:**
```json
{
  "startPlace": "Shanghai",
  "endPlace": "Beijing",
  "departureTime": "2020-01-01"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Trip query rejected: departureTime cannot be in the past",
  "data": {
    "isInjected": true,
    "faultName": "PAST_DATE_FAULT",
    "message": "Trip query rejected: departureTime cannot be in the past",
    "details": "2020-01-01"
  }
}
```

---

### 10. INVALID_DATE_FORMAT_FAULT

**API:** `POST /api/v1/travelservice/trips/left`

**Description:** Rejects trip query when date format is invalid.

**Trigger Conditions:**
- `departureTime` is not in format `yyyy-MM-dd`

**Sample Input:**
```json
{
  "startPlace": "Shanghai",
  "endPlace": "Beijing",
  "departureTime": "01/15/2025"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Trip query rejected: departureTime must be in format yyyy-MM-dd",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_DATE_FORMAT_FAULT",
    "message": "Trip query rejected: departureTime must be in format yyyy-MM-dd",
    "details": "01/15/2025"
  }
}
```

---

## ts-auth-service

### 11. INVALID_USERNAME_FORMAT_FAULT

**API:** `POST /api/v1/users/login`

**Description:** Rejects login when username format is invalid.

**Trigger Conditions:**
- `username` is null or empty
- `username` length < 4 or > 20 characters

**Sample Input:**
```json
{
  "username": "abc",
  "password": "password123"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Login rejected: username length must be between 4 and 20 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_USERNAME_FORMAT_FAULT",
    "message": "Login rejected: username length must be between 4 and 20 characters",
    "details": "Username length: 3"
  }
}
```

---

### 12. INVALID_PASSWORD_LENGTH_FAULT

**API:** `POST /api/v1/users/login`

**Description:** Rejects login when password is too short.

**Trigger Conditions:**
- `password` is null or length < 6 characters

**Sample Input:**
```json
{
  "username": "user123",
  "password": "12345"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Login rejected: password must be at least 6 characters",
  "data": {
    "isInjected": true,
    "faultName": "INVALID_PASSWORD_LENGTH_FAULT",
    "message": "Login rejected: password must be at least 6 characters",
    "details": "Password length: 5"
  }
}
```

---

## ts-admin-travel-service

### 13. INVALID_TRIP_ID_FORMAT_FAULT

**API:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`

**Description:** Rejects trip deletion when tripId format is invalid.

**Trigger Conditions:**
- `tripId` is null or empty

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

### 14. INVALID_TRIP_ID_LENGTH_FAULT

**API:** `DELETE /api/v1/admintravelservice/admintravel/{tripId}`

**Description:** Rejects trip deletion when tripId length is invalid.

**Trigger Conditions:**
- `tripId` length < 4 or > 20 characters

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

### 15. INSUFFICIENT_STATIONS_FAULT

**API:** `POST /api/v1/adminrouteservice/adminroute`

**Description:** Rejects route creation when station list is insufficient.

**Trigger Conditions:**
- `stationList` is null or empty
- Number of stations < 2

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

### 16. DUPLICATE_STATIONS_FAULT

**API:** `POST /api/v1/adminrouteservice/adminroute`

**Description:** Rejects route creation when station list contains duplicates.

**Trigger Conditions:**
- `stationList` contains duplicate stations (case-insensitive)

**Sample Input:**
```json
{
  "id": "route-123",
  "startStation": "Shanghai",
  "endStation": "Beijing",
  "stationList": "Shanghai,Beijing,shanghai"
}
```

**Sample Output:**
```json
{
  "status": 0,
  "msg": "Route creation rejected: station list cannot contain duplicate stations",
  "data": {
    "isInjected": true,
    "faultName": "DUPLICATE_STATIONS_FAULT",
    "message": "Route creation rejected: station list cannot contain duplicate stations",
    "details": "Total stations: 3, Unique stations: 2"
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

## Summary

**Total Services Injected:** 7
- ts-admin-order-service
- ts-admin-basic-info-service
- ts-travel-plan-service
- ts-travel-service
- ts-auth-service
- ts-admin-travel-service
- ts-admin-route-service

**Total Fault Types:** 16

**Injected APIs:**
1. POST /api/v1/adminorderservice/adminorder
2. PUT /api/v1/adminorderservice/adminorder
3. DELETE /api/v1/adminorderservice/adminorder/{orderId}/{trainNumber}
4. POST /api/v1/adminbasicservice/adminbasic/prices
5. POST /api/v1/travelplanservice/travelPlan/minStation
6. POST /api/v1/travelservice/trips/left
7. POST /api/v1/users/login
8. DELETE /api/v1/admintravelservice/admintravel/{tripId}
9. POST /api/v1/adminrouteservice/adminroute

