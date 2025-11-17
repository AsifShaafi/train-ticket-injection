package travel.controller;

import edu.fudan.common.entity.TravelInfo;
import edu.fudan.common.entity.TripAllDetailInfo;
import edu.fudan.common.entity.TripInfo;
import edu.fudan.common.entity.TripResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.fudan.common.entity.TravelInfo;
import edu.fudan.common.util.Response;
import travel.entity.*;
import travel.service.TravelService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/travelservice")

public class TravelController {

    @Autowired
    private TravelService travelService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelController.class);

    @GetMapping(path = "/welcome")
    public String home(@RequestHeader HttpHeaders headers) {
        return "Welcome to [ Travel Service ] !";
    }

    @GetMapping(value = "/train_types/{tripId}")
    public HttpEntity getTrainTypeByTripId(@PathVariable String tripId,
                                           @RequestHeader HttpHeaders headers) {
        // TrainType
        TravelController.LOGGER.info("[getTrainTypeByTripId][Get train Type by Trip id][TripId: {}]", tripId);
        return ok(travelService.getTrainTypeByTripId(tripId, headers));
    }

    @GetMapping(value = "/routes/{tripId}")
    public HttpEntity getRouteByTripId(@PathVariable String tripId,
                                       @RequestHeader HttpHeaders headers) {
        TravelController.LOGGER.info("[getRouteByTripId][Get Route By Trip ID][TripId: {}]", tripId);
        //Route
        return ok(travelService.getRouteByTripId(tripId, headers));
    }

    @PostMapping(value = "/trips/routes")
    public HttpEntity getTripsByRouteId(@RequestBody ArrayList<String> routeIds,
                                        @RequestHeader HttpHeaders headers) {
        // ArrayList<ArrayList<Trip>>
        TravelController.LOGGER.info("[getTripByRoute][Get Trips by Route ids][RouteIds: {}]", routeIds.size());
        return ok(travelService.getTripByRoute(routeIds, headers));
    }

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trips")
    public HttpEntity<?> createTrip(@RequestBody TravelInfo routeIds, @RequestHeader HttpHeaders headers) {
        // null
        TravelController.LOGGER.info("[create][Create trip][TripId: {}]", routeIds.getTripId());
        return new ResponseEntity<>(travelService.create(routeIds, headers), HttpStatus.CREATED);
    }

    /**
     * Return Trip only, no left ticket information
     *
     * @param tripId  trip id
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @GetMapping(value = "/trips/{tripId}")
    public HttpEntity retrieve(@PathVariable String tripId, @RequestHeader HttpHeaders headers) {
        // Trip
        TravelController.LOGGER.info("[retrieve][Retrieve trip][TripId: {}]", tripId);
        return ok(travelService.retrieve(tripId, headers));
    }

    @CrossOrigin(origins = "*")
    @PutMapping(value = "/trips")
    public HttpEntity updateTrip(@RequestBody TravelInfo info, @RequestHeader HttpHeaders headers) {
        // Trip
        TravelController.LOGGER.info("[update][Update trip][TripId: {}]", info.getTripId());
        return ok(travelService.update(info, headers));
    }

    @CrossOrigin(origins = "*")
    @DeleteMapping(value = "/trips/{tripId}")
    public HttpEntity deleteTrip(@PathVariable String tripId, @RequestHeader HttpHeaders headers) {
        // string
        TravelController.LOGGER.info("[delete][Delete trip][TripId: {}]", tripId);
        return ok(travelService.delete(tripId, headers));
    }

    /**
     * Return Trips and the remaining tickets
     *
     * @param info    trip info
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trips/left")
    public HttpEntity queryInfo(@RequestBody TripInfo info, @RequestHeader HttpHeaders headers) {
        if (info.getStartPlace() == null || info.getStartPlace().length() == 0 ||
                info.getEndPlace() == null || info.getEndPlace().length() == 0 ||
                info.getDepartureTime() == null) {
            TravelController.LOGGER.info("[query][Travel Query Fail][Something null]");
            ArrayList<TripResponse> errorList = new ArrayList<>();
            return ok(errorList);
        }
        
        TravelController.LOGGER.info("[query][Query TripResponse][Start: {}, End: {}, Date: {}]", 
            info.getStartPlace(), info.getEndPlace(), info.getDepartureTime());
        
        // INJECTED FAULT: Validate station name length (must be between 2 and 50 characters)
        if (info.getStartPlace().trim().length() < 2 || info.getStartPlace().trim().length() > 50) {
            TravelController.LOGGER.warn("[query][INJECTED FAULT][INVALID_STATION_LENGTH_FAULT] startPlace length invalid: {}", info.getStartPlace().trim().length());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_STATION_LENGTH_FAULT", 
                "Trip query rejected: startPlace length must be between 2 and 50 characters",
                String.format("startPlace: '%s', Length: %d", info.getStartPlace().trim(), info.getStartPlace().trim().length())
            );
            return ResponseEntity.badRequest().body(new Response<>(0, "Trip query rejected: startPlace length must be between 2 and 50 characters", faultResponse));
        }
        
        if (info.getEndPlace().trim().length() < 2 || info.getEndPlace().trim().length() > 50) {
            TravelController.LOGGER.warn("[query][INJECTED FAULT][INVALID_STATION_LENGTH_FAULT] endPlace length invalid: {}", info.getEndPlace().trim().length());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_STATION_LENGTH_FAULT", 
                "Trip query rejected: endPlace length must be between 2 and 50 characters",
                String.format("endPlace: '%s', Length: %d", info.getEndPlace().trim(), info.getEndPlace().trim().length())
            );
            return ResponseEntity.badRequest().body(new Response<>(0, "Trip query rejected: endPlace length must be between 2 and 50 characters", faultResponse));
        }
        
        // INJECTED FAULT: Validate date format (basic check for valid date string)
        if (info.getDepartureTime() != null && !info.getDepartureTime().trim().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setLenient(false);
                Date departureDate = sdf.parse(info.getDepartureTime());
                
                // Check if year is in valid range (2000-2100)
                String dateStr = info.getDepartureTime().trim();
                if (dateStr.length() >= 4) {
                    int year = Integer.parseInt(dateStr.substring(0, 4));
                    if (year < 2000 || year > 2100) {
                        TravelController.LOGGER.warn("[query][INJECTED FAULT][INVALID_YEAR_RANGE_FAULT] departureTime year out of range: {}", year);
                        FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                            true, 
                            "INVALID_YEAR_RANGE_FAULT", 
                            "Trip query rejected: departureTime year must be between 2000 and 2100",
                            String.format("Year: %d", year)
                        );
                        return ResponseEntity.badRequest().body(new Response<>(0, "Trip query rejected: departureTime year must be between 2000 and 2100", faultResponse));
                    }
                }
            } catch (Exception e) {
                TravelController.LOGGER.warn("[query][INJECTED FAULT][INVALID_DATE_FORMAT_FAULT] Invalid date format: {}", info.getDepartureTime());
                FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                    true, 
                    "INVALID_DATE_FORMAT_FAULT", 
                    "Trip query rejected: departureTime must be in format yyyy-MM-dd",
                    info.getDepartureTime()
                );
                return ResponseEntity.badRequest().body(new Response<>(0, "Trip query rejected: departureTime must be in format yyyy-MM-dd", faultResponse));
            }
        }
        
        return ok(travelService.queryByBatch(info, headers));
    }

    /**
     * Return Trips and the remaining tickets
     *
     * @param info    trip info
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trips/left_parallel")
    public HttpEntity queryInfoInparallel(@RequestBody TripInfo info, @RequestHeader HttpHeaders headers) {
        if (info.getStartPlace() == null || info.getStartPlace().length() == 0 ||
                info.getEndPlace() == null || info.getEndPlace().length() == 0 ||
                info.getDepartureTime() == null) {
            TravelController.LOGGER.info("[queryInParallel][Travel Query Fail][Something null]");
            ArrayList<TripResponse> errorList = new ArrayList<>();
            return ok(errorList);
        }
        TravelController.LOGGER.info("[queryInParallel][Query TripResponse]");
        return ok(travelService.queryInParallel(info, headers));
    }

    /**
     * Return a Trip and the remaining
     *
     * @param gtdi    trip all detail info
     * @param headers headers
     * @return HttpEntity
     */
    @CrossOrigin(origins = "*")
    @PostMapping(value = "/trip_detail")
    public HttpEntity getTripAllDetailInfo(@RequestBody TripAllDetailInfo gtdi, @RequestHeader HttpHeaders headers) {
        // TripAllDetailInfo
        // TripAllDetail tripAllDetail
        TravelController.LOGGER.info("[getTripAllDetailInfo][Get trip detail][TripId: {}]", gtdi.getTripId());
        return ok(travelService.getTripAllDetailInfo(gtdi, headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/trips")
    public HttpEntity queryAll(@RequestHeader HttpHeaders headers) {
        // List<Trip>
        TravelController.LOGGER.info("[queryAll][Query all trips]");
        return ok(travelService.queryAll(headers));
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/admin_trip")
    public HttpEntity adminQueryAll(@RequestHeader HttpHeaders headers) {
        // ArrayList<AdminTrip>
        TravelController.LOGGER.info("[adminQueryAll][Admin query all trips]");
        return ok(travelService.adminQueryAll(headers));
    }

}
