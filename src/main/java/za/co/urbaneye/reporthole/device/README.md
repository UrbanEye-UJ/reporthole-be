# Module: device

Manages dashcam device registration. Each physical dashcam registers with the backend and receives a long-lived device token it uses to authenticate frame-submission requests to the `inference` module.

---

## Package structure

```
device/
├── controller/
│   └── DeviceController.java          — POST /devices/token/generate, GET /devices, DELETE /devices/token/{id}
├── dto/
│   ├── DeviceTokenResponse.java       — returns { deviceToken: "..." } on generation
│   └── DeviceSummaryResponse.java     — { deviceId, tokenPreview, createdAt } returned by GET /devices
├── entity/
│   └── DashcamDevice.java             — device ID, token hash, registered user, timestamps
├── exception/
│   └── DeviceServiceException.java
├── repository/
│   └── DashcamDeviceRepository.java
└── service/
    ├── interfaces/
    │   └── IDeviceService.java
    └── impl/
        └── DeviceServiceImpl.java     — generates token, hashes it, persists DashcamDevice
```

---

## Flow

1. An authenticated civilian calls `POST /devices/token/generate` (no request body — the user is read from the JWT).
2. `DeviceServiceImpl` generates a random UUID token, stores it **in plaintext** in `DashcamDevice` (see note below), and returns it to the client. Shown once — the frontend saves it to the device's `localStorage`.
3. The dashcam stores the token locally and sends it as a bearer token to `/incidents/**` (e.g. `POST /incidents/create`) and `POST /inference/frames`. `JwtAuthenticationFilter` resolves it via `DashcamDeviceRepository.findByDeviceToken`.
4. `GET /devices` (JWT only) lets the civilian see every device they've registered — id, a short `tokenPreview` (last 8 characters), and `createdAt`. The full token is never returned again after step 2.
5. `DELETE /devices/token/{id}` revokes a token by deleting its `DashcamDevice` row.

Device tokens are long-lived (no expiry by design for embedded hardware) but can be revoked at any time from the civilian profile page.

**Note:** unlike an earlier version of this doc, tokens are **not** hashed at rest — `DashcamDevice.deviceToken` stores the raw value so `findByDeviceToken` can look it up directly. Worth hardening later (store a hash, look up by it) since a DB read currently exposes the live secret.

---

## Tests

| Test class | Type |
|-----------|------|
| `DeviceServiceImplTest` | Unit (Mockito) |
| `DeviceControllerTest` | Unit (Mockito) |
| `DeviceIntegrationTest` | Integration (SpringBootTest + H2) |
