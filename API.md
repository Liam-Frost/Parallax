# Parallax API Contract (Phase 1)

Status: Active (authoritative for Phase 1)

This document defines the target API contract for the Phase 1 migration to Spring Boot.
All routes are rooted at `/api` and preserve the current path structure to avoid breaking
the frontend migration. After stabilization, routes will be versioned under `/api/v1`.

---

## 1. Conventions

### 1.1 Authentication
- Access Token: `Authorization: Bearer <jwt>`
- Access token is required for authenticated endpoints (unless marked as public)
- Client must not send `username` in query/body for authentication; the server derives
  identity from JWT `sub`

### 1.2 CSRF
- All write operations (POST/PUT/PATCH/DELETE) require `X-CSRF-Token`
- Double Submit Cookie is used. The server sets `XSRF-TOKEN` cookie and expects
  the same value in the header.

### 1.3 Refresh Cookie
- Refresh token is stored as HttpOnly cookie, rotation enabled
- SameSite=Lax, Secure, Path=/api/auth/refresh
- Cookie Domain is not set (host-only)

### 1.4 Error Format (Standard)
```json
{
  "success": false,
  "errorCode": "SOME_CODE",
  "message": "Human readable message"
}
```

---

## 2. Auth

### 2.1 Login
`POST /api/auth/login`

Request Body:
```json
{
  "identifier": "liam@example.com",
  "password": "Secret123"
}
```

Response:
```json
{
  "success": true,
  "accessToken": "<jwt>",
  "expiresIn": 900,
  "user": {
    "id": "...",
    "email": "liam@example.com",
    "displayName": "Liam Frost",
    "role": "USER"
  }
}
```

Side Effects:
- Sets refresh cookie
- Creates refresh session record

---

### 2.2 Register
`POST /api/auth/register`

Request Body:
```json
{
  "email": "liam@example.com",
  "password": "Secret123",
  "firstName": "Liam",
  "lastName": "Frost",
  "country": "CA",
  "birthYear": 2003,
  "birthMonth": 1,
  "birthDay": 15,
  "phoneCountry": "+1",
  "phone": "6041234567",
  "contactMethod": "text"
}
```

Response:
```json
{
  "success": true,
  "accessToken": "<jwt>",
  "expiresIn": 900,
  "user": {
    "id": "...",
    "email": "liam@example.com",
    "displayName": "Liam Frost",
    "role": "USER"
  }
}
```

Side Effects:
- Sets refresh cookie
- Creates refresh session record

---

### 2.3 Refresh
`POST /api/auth/refresh`

Request:
- Refresh cookie (sent automatically with `credentials: include`)

Response:
```json
{
  "success": true,
  "accessToken": "<jwt>",
  "expiresIn": 900
}
```

Side Effects:
- Rotates refresh cookie
- Updates refresh session (or creates a new session)

---

### 2.4 Logout (current session)
`POST /api/auth/logout`

Request:
- Access token: `Authorization: Bearer <jwt>`
- Client should send `credentials: include` so the refresh cookie can be cleared

Response:
```json
{ "success": true }
```

Side Effects:
- Revokes current session (by `sid` in access token)
- Clears refresh cookie

---

### 2.5 Logout All
`POST /api/auth/logout_all`

Request:
- Access token: `Authorization: Bearer <jwt>`
- Client should send `credentials: include` so the refresh cookie can be cleared

Response:
```json
{ "success": true }
```

Side Effects:
- Revokes all sessions for the current user
- Clears refresh cookie

---

## 3. Account

### 3.1 Get Current User
`GET /api/account/me`

Response:
```json
{
  "email": "liam@example.com",
  "displayName": "Liam Frost",
  "firstName": "Liam",
  "lastName": "Frost",
  "country": "CA",
  "birthYear": 2003,
  "birthMonth": 1,
  "birthDay": 15,
  "phoneCountry": "+1",
  "phone": "6041234567",
  "contactMethod": "text"
}
```

---

### 3.2 Update Contact Info
`POST /api/account/contact`

Request Body:
```json
{
  "email": "new@example.com",
  "phoneCountry": "+1",
  "phone": "7781239876",
  "currentPassword": "Secret123"
}
```

Response:
```json
{ "success": true }
```

---

### 3.3 Change Password
`POST /api/account/password`

Request Body:
```json
{
  "oldPassword": "Secret123",
  "newPassword": "NewPass456",
  "confirmPassword": "NewPass456",
  "captcha": "ABCD"
}
```

Response:
```json
{ "success": true }
```

---

### 3.4 Delete Account
`DELETE /api/account`

Notes:
- Admin accounts cannot be deleted.

Request Body:
```json
{ "currentPassword": "Secret123" }
```

Response:
```json
{ "success": true }
```

Side Effects:
- Deletes user
- Cascades to delete owned vehicles
- Revokes all refresh sessions for the user

---

## 4. Vehicles

### 4.1 List Vehicles
`GET /api/vehicles`

Response (USER):
```json
{
  "vehicles": [
    {
      "licenseNumber": "ABC1234",
      "make": "Toyota",
      "model": "Corolla",
      "year": 2020,
      "blacklisted": false
    }
  ]
}
```

Response (ADMIN):
```json
{
  "vehicles": [
    {
      "licenseNumber": "ABC1234",
      "make": "Toyota",
      "model": "Corolla",
      "year": 2020,
      "blacklisted": false,
      "owner": {
        "email": "liam@example.com",
        "phone": "+16041234567"
      }
    }
  ]
}
```

---

### 4.2 Add Vehicle
`POST /api/vehicles`

Request Body:
```json
{
  "licenseNumber": "ABC1234",
  "make": "Toyota",
  "model": "Corolla",
  "year": 2020
}
```

Response:
```json
{ "success": true }
```

---

### 4.3 Delete Vehicle
`DELETE /api/vehicles`

Request Body:
```json
{ "licenseNumber": "ABC1234" }
```

Response:
```json
{ "success": true }
```

---

### 4.4 Update Blacklist Status (ADMIN)
`POST /api/vehicles/blacklist`

Request Body:
```json
{
  "licenseNumber": "ABC1234",
  "blacklisted": true
}
```

Response:
```json
{ "success": true }
```

---

## 5. Query

### 5.1 Text Query (Public)
`GET /api/vehicles/query?license=ABC1234`

Response (found):
```json
{
  "success": true,
  "found": true,
  "licenseNumber": "ABC1234",
  "blacklisted": false
}
```

Response (not found):
```json
{
  "success": true,
  "found": false,
  "licenseNumber": "ABC1234",
  "blacklisted": false
}
```

---

### 5.2 Image Query (Public)
`POST /api/vehicles/query-image`

Request:
- Multipart form data
- Field: `image`

Response (plate found):
```json
{
  "success": true,
  "plateFound": true,
  "licenseNumber": "ABC1234",
  "foundInSystem": true,
  "blacklisted": true,
  "confidence": 0.92
}
```

Response (no plate):
```json
{
  "success": true,
  "plateFound": false,
  "message": "No readable license plate was found in the image."
}
```

Error (OCR unavailable):
```json
{
  "success": false,
  "errorCode": "OCR_UNAVAILABLE",
  "message": "Image recognition failed."
}
```

---

## 6. Health

### 6.1 Health Check
`GET /api/health`

Response:
```json
{ "status": "ok" }
```
