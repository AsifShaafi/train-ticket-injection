# Fault Injection Summary - ts-auth-service

## Injected Faults

### 1. INVALID_USERNAME_FORMAT_FAULT
- **Endpoint:** `POST /api/v1/users/login`
- **Triggers:** username is null, empty, or length not between 4 and 20 characters
- **Test:** Send login with `username: "abc"` (too short) or `username: "a12345678901234567890"` (too long)

### 2. INVALID_PASSWORD_LENGTH_FAULT
- **Endpoint:** `POST /api/v1/users/login`
- **Triggers:** password is null or length less than 6 characters
- **Test:** Send login with `password: "12345"` or `password: null`

## Files Modified
- `src/main/java/auth/entity/FaultInjectionResponse.java` (NEW)
- `src/main/java/auth/controller/UserController.java` (MODIFIED)

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

