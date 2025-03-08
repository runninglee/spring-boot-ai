package vip.lycheer.ai.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.lycheer.ai.service.BookingTools;
import vip.lycheer.ai.service.FlightBookingService;

import java.util.List;

@RestController
@CrossOrigin
public class BookingController {

    private final FlightBookingService flightBookingService;

    public BookingController(FlightBookingService flightBookingService) {
        this.flightBookingService = flightBookingService;
    }

    @CrossOrigin
    @GetMapping(value = "/booking/list")
    public List<BookingTools.BookingDetails> getBookings() {
        return flightBookingService.getBookings();
    }

}
