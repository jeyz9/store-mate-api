# Test Report

- **Date:** 2026-10-12 (Asia/Bangkok)
- **Command:** `.\mvnw.cmd test`
- **Result:** `BUILD SUCCESS`
- **Test classes:** 8
- **Tests:** 24 passed, 0 failures, 0 errors, 0 skipped

## Results by test file

| Test file | Tests | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| `AuthControllerTest.java` | 4 | 0 | 0 | 0 | Passed |
| `AuthServiceTest.java` | 3 | 0 | 0 | 0 | Passed |
| `NotificationServiceTest.java` | 5 | 0 | 0 | 0 | Passed |
| `OrderServiceTest.java` | 2 | 0 | 0 | 0 | Passed |
| `OwnerDashboardServiceTest.java` | 6 | 0 | 0 | 0 | Passed |
| `PaymentServiceTest.java` | 2 | 0 | 0 | 0 | Passed |
| `ProductServiceTest.java` | 1 | 0 | 0 | 0 | Passed |
| `StoreMateApiApplicationTests.java` | 1 | 0 | 0 | 0 | Passed |
| **Total** | **24** | **0** | **0** | **0** | **Passed** |

## Notes

- `StoreMateApiApplicationTests` successfully loaded the Spring application context using the `test` profile and the in-memory H2 database.
- `AuthControllerTest` confirms invalid registration phone numbers and passwords receive HTTP 400 with the expected validation message; a valid ten-digit phone and password containing an allowed special character proceed with HTTP 201.
- The latest targeted run (`.\mvnw.cmd -Dtest=AuthControllerTest test`) passed all 4 controller tests. The other 20 tests passed in the earlier full-suite run.
- Surefire also emitted a Mockito inline-mock-maker self-attachment warning. It did not affect the run; all tests completed successfully.
