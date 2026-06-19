package kolbooking.datn.booking.service;

import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EC-1: State machine enforces that a booking cannot be transitioned twice
 * to the same terminal/disbursement state. This is the lightweight guard that
 * prevents double-disbursement if two threads reach transition() concurrently
 * after the pessimistic lock re-reads the same committed status.
 *
 * EC-1 deeper: the DB-level lock in BookingRepository.findByIdForUpdate ensures
 * only one thread at a time executes the transition body; this test covers the
 * "already in target" guard that fires for the second caller.
 */
class BookingStateMachineTest {

    // ── Self-transition (same → same) always throws ───────────────────────────

    @ParameterizedTest(name = "no self-transition from {0}")
    @EnumSource(BookingStatus.class)
    void ensureTransition_selfTransition_throws(BookingStatus status) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> BookingStateMachine.ensureTransition(status, status));
        assertTrue(ex.getMessage().contains("already in"));
    }

    // ── EC-1 guard: COMPLETED is a terminal state → no further transitions ────

    @Test
    void ensureTransition_fromCompleted_toAny_throws() {
        for (BookingStatus target : BookingStatus.values()) {
            if (target == BookingStatus.COMPLETED) continue; // covered by self-transition
            assertThrows(BusinessException.class,
                    () -> BookingStateMachine.ensureTransition(BookingStatus.COMPLETED, target),
                    "COMPLETED → " + target + " must be rejected");
        }
    }

    // ── Happy path transitions (spot-check key escrow lifecycle steps) ─────────

    @Test
    void escrowLifecycle_happyPath_noThrows() {
        // PENDING → ACCEPTED
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.PENDING, BookingStatus.ACCEPTED));
        // ACCEPTED → IN_PROGRESS (payment confirmed)
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS));
        // IN_PROGRESS → DELIVERED (KOL submits)
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.IN_PROGRESS, BookingStatus.DELIVERED));
        // DELIVERED → COMPLETED (brand approves → triggers disbursement)
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.DELIVERED, BookingStatus.COMPLETED));
    }

    @Test
    void rejectDelivery_triggersRefund_path() {
        // DELIVERED → DELIVERY_REJECTED (brand rejects → triggers refund)
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.DELIVERED, BookingStatus.DELIVERY_REJECTED));
    }

    @Test
    void requestRevision_returnsToInProgress() {
        assertDoesNotThrow(() -> BookingStateMachine.ensureTransition(
                BookingStatus.DELIVERED, BookingStatus.IN_PROGRESS));
    }

    // ── EC-1: DELIVERY_REJECTED is terminal — cannot be re-approved afterwards ─

    @Test
    void deliveryRejected_isTerminal_cannotBeApproved() {
        assertThrows(BusinessException.class,
                () -> BookingStateMachine.ensureTransition(
                        BookingStatus.DELIVERY_REJECTED, BookingStatus.COMPLETED),
                "DELIVERY_REJECTED must not transition to COMPLETED");
    }

    // ── EC-2 cron safety: COMPLETED → COMPLETED throws, so auto-complete cron
    //    will get a BusinessException and log.warn(), not double-disburse ────────

    @Test
    void autoCompleteCron_onAlreadyCompleted_throwsNotSilentlyPasses() {
        assertThrows(BusinessException.class,
                () -> BookingStateMachine.ensureTransition(
                        BookingStatus.COMPLETED, BookingStatus.COMPLETED));
    }
}
