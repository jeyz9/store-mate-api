package com.sm.jeyz9.storemateapi;

import com.sm.jeyz9.storemateapi.dto.OwnerDashboardDTO;
import com.sm.jeyz9.storemateapi.exceptions.WebException;
import com.sm.jeyz9.storemateapi.repository.OrderRepository;
import com.sm.jeyz9.storemateapi.repository.OwnerDashboardRepository;
import com.sm.jeyz9.storemateapi.repository.ProductRepository;
import com.sm.jeyz9.storemateapi.repository.ZipcodeRepository;
import com.sm.jeyz9.storemateapi.services.OwnerDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OwnerDashboardServiceTest {

    private RecordingOwnerDashboardRepository ownerDashboardRepository;
    private OwnerDashboardService ownerDashboardService;

    @BeforeEach
    void setUp() {
        ownerDashboardRepository = new RecordingOwnerDashboardRepository();
        ownerDashboardService = new OwnerDashboardService(
                ownerDashboardRepository,
                unusedDependency(ProductRepository.class),
                unusedDependency(OrderRepository.class),
                unusedDependency(ZipcodeRepository.class)
        );
    }

    @Test
    void getAdminDashboard_normalizesPeriodsAndPassesSelectedYearRange() {
        LocalDate selectedDate = LocalDate.of(2025, 6, 12);
        OwnerDashboardDTO expected = new OwnerDashboardDTO();
        ownerDashboardRepository.result = Optional.of(expected);

        OwnerDashboardDTO actual = ownerDashboardService.getAdminDashboard(
                " month ", selectedDate, " quarter ", 2020, 2026
        );

        assertSame(expected, actual);
        assertEquals("MONTH", ownerDashboardRepository.salesPeriod);
        assertEquals(selectedDate, ownerDashboardRepository.selectedDate);
        assertEquals("QUARTER", ownerDashboardRepository.comparePeriod);
        assertEquals(2020, ownerDashboardRepository.fromYear);
        assertEquals(2026, ownerDashboardRepository.toYear);
        assertEquals(1, ownerDashboardRepository.callCount);
    }

    @Test
    void getAdminDashboard_defaultsComparisonYearsFromSelectedDate() {
        LocalDate selectedDate = LocalDate.of(2024, 3, 15);

        ownerDashboardService.getAdminDashboard(
                "year", selectedDate, "month", null, null
        );

        assertEquals(2023, ownerDashboardRepository.fromYear);
        assertEquals(2024, ownerDashboardRepository.toYear);
    }

    @Test
    void getAdminDashboard_returnsEmptyDtoWhenRepositoryHasNoResult() {
        ownerDashboardRepository.result = Optional.empty();

        OwnerDashboardDTO actual = ownerDashboardService.getAdminDashboard(
                "day", LocalDate.of(2026, 1, 1), "year", 2025, 2026
        );

        assertNotNull(actual);
        assertNull(actual.getTotalRevenue());
        assertNull(actual.getYearActiveIncomeChart());
    }

    @Test
    void getAdminDashboard_rejectsUnsupportedSalesPeriod() {
        WebException exception = assertThrows(WebException.class, () ->
                ownerDashboardService.getAdminDashboard(
                        "WEEK", LocalDate.of(2026, 1, 1), "MONTH", 2025, 2026
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Unsupported dashboard period: WEEK", exception.getMessage());
        assertEquals(0, ownerDashboardRepository.callCount);
    }

    @Test
    void getAdminDashboard_rejectsUnsupportedComparisonPeriod() {
        WebException exception = assertThrows(WebException.class, () ->
                ownerDashboardService.getAdminDashboard(
                        "YEAR", LocalDate.of(2026, 1, 1), "DAY", 2025, 2026
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Unsupported dashboard period: DAY", exception.getMessage());
        assertEquals(0, ownerDashboardRepository.callCount);
    }

    @Test
    void getAdminDashboard_rejectsComparisonRangeWhenStartYearIsAfterEndYear() {
        WebException exception = assertThrows(WebException.class, () ->
                ownerDashboardService.getAdminDashboard(
                        "YEAR", LocalDate.of(2026, 1, 1), "YEAR", 2027, 2026
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(
                "compareFromYear must be less than or equal to compareToYear",
                exception.getMessage()
        );
        assertEquals(0, ownerDashboardRepository.callCount);
    }

    private static <T> T unusedDependency(Class<T> dependencyType) {
        return dependencyType.cast(Proxy.newProxyInstance(
                dependencyType.getClassLoader(),
                new Class<?>[]{dependencyType},
                (proxy, method, args) -> null
        ));
    }

    private static class RecordingOwnerDashboardRepository extends OwnerDashboardRepository {
        private Optional<OwnerDashboardDTO> result = Optional.of(new OwnerDashboardDTO());
        private String salesPeriod;
        private LocalDate selectedDate;
        private String comparePeriod;
        private int fromYear;
        private int toYear;
        private int callCount;

        private RecordingOwnerDashboardRepository() {
            super(null, null);
        }

        @Override
        public Optional<OwnerDashboardDTO> findOwnerDashboard(
                String salesPeriod,
                LocalDate selectedDate,
                String comparePeriod,
                int fromYear,
                int toYear
        ) {
            this.salesPeriod = salesPeriod;
            this.selectedDate = selectedDate;
            this.comparePeriod = comparePeriod;
            this.fromYear = fromYear;
            this.toYear = toYear;
            callCount++;
            return result;
        }
    }
}
