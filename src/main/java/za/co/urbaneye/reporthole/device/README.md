# Module: device

Manages dashcam device registration. Each physical dashcam registers with the backend and receives a long-lived device token it uses to authenticate frame-submission requests to the `inference` module.

---

## Package structure

```
device/
├── controller/
│   └── DeviceController.java          — POST /devices/register
├── dto/
│   └── DeviceTokenResponse.java       — returns { deviceToken: "..." } on registration
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

1. An authenticated civilian calls `POST /devices/register` with device metadata.
2. `DeviceServiceImpl` generates a secure random token, stores the SHA-256 hash in `DashcamDevice`, and returns the plain token to the client.
3. The dashcam stores the token locally and sends it as a bearer token when submitting frames to `POST /inference/frame`.
4. The `inference` module validates the token against the stored hash before processing any frame.

Device tokens are long-lived (no expiry by design for embedded hardware) but can be revoked by deleting the `DashcamDevice` record.

---

## Tests

| Test class | Type |
|-----------|------|
| `DeviceServiceImplTest` | Unit (Mockito) |
| `DeviceControllerTest` | Unit (Mockito) |
| `DeviceIntegrationTest` | Integration (SpringBootTest + H2) |
