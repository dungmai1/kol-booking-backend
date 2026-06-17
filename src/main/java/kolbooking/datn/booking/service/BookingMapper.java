package kolbooking.datn.booking.service;

import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingDeliverable;
import kolbooking.datn.booking.domain.BookingMessage;
import kolbooking.datn.booking.dto.BookingMessageResponse;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.SubmittedDeliverableDto;

import java.util.List;

public final class BookingMapper {

    private BookingMapper() {}

    public static BookingResponse toDto(Booking b) {
        return toDto(b, List.of());
    }

    public static BookingResponse toDto(Booking b, List<BookingDeliverable> deliverables) {
        List<SubmittedDeliverableDto> mapped = deliverables.stream()
                .map(d -> new SubmittedDeliverableDto(
                        d.getId(), d.getType(), d.getPlatform(),
                        d.getSubmittedUrl(), d.getNote(), d.getSubmittedAt(), d.getStatus()))
                .toList();
        return new BookingResponse(
                b.getId(), b.getBrandProfileId(), b.getKolProfileId(),
                b.getCampaignTitle(), b.getCampaignBrief(), b.getDeliverables(),
                b.getBudget(), b.getPlatformFeePercent(), b.getPlatformFeeAmount(), b.getKolNetAmount(),
                b.getStartDate(), b.getEndDate(),
                b.getStatus(), b.getRejectReason(), b.getCancelReason(),
                b.getInvoiceUrl(), b.getCreatedAt(), b.getUpdatedAt(),
                mapped
        );
    }

    public static BookingMessageResponse toDto(BookingMessage m) {
        return new BookingMessageResponse(
                m.getId(), m.getBookingId(), m.getSenderUserId(),
                m.getContent(), m.getAttachmentUrl(), m.getCreatedAt()
        );
    }
}
