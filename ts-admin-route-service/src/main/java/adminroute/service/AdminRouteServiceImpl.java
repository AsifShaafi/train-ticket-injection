package adminroute.service;

import adminroute.entity.FaultInjectionResponse;
import edu.fudan.common.entity.Route;
import edu.fudan.common.entity.RouteInfo;
import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fdse
 */
@Service
public class AdminRouteServiceImpl implements AdminRouteService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DiscoveryClient discoveryClient;

    public static final Logger logger = LoggerFactory.getLogger(AdminRouteServiceImpl.class);

    private String getServiceUrl(String serviceName) {
        return "http://" + serviceName;
    }

    @Override
    public Response getAllRoutes(HttpHeaders headers) {

        HttpEntity requestEntity = new HttpEntity(null);
        String route_service_url = getServiceUrl("ts-route-service");
        ResponseEntity<Response> re = restTemplate.exchange(
                 route_service_url + "/api/v1/routeservice/routes",
                HttpMethod.GET,
                requestEntity,
                Response.class);
        if (re.getStatusCode() != HttpStatus.ACCEPTED) {
            logger.error("[getAllRoutes][receive response][Get routes error][response code: {}]", re.getStatusCodeValue());
        }
        return re.getBody();

    }

    @Override
    public Response createAndModifyRoute(RouteInfo request, HttpHeaders headers) {
        logger.info("[createAndModifyRoute][Create and modify route][route id: {}]", request.getId());
        
        // EXPERIMENT MUTANT (silent-acceptance, for the MIST trace-oracle demo).
        // CORRECT behavior is to REJECT insufficient-station routes with
        // Response(0)+faultName -> HTTP 400 (as the other validations below still
        // do). Here we deliberately ACCEPT the invalid route and report success
        // (status 1, NO faultName) -> HTTP 200, so the bad input is silently
        // accepted. This turns INSUFFICIENT_STATIONS_FAULT (one of the 10 tracked
        // faults) into a silent-acceptance bug: a status/fault-name oracle misses
        // it (looks like success), but the intent-aware trace oracle
        // (SilentAcceptanceInvariant) catches it. Revert to restore correctness.
        List<String> stations = request.getStations();
        if (stations == null || stations.size() < 2) {
            logger.warn("[createAndModifyRoute][SILENT_ACCEPTANCE MUTANT] insufficient stations accepted as success (should have been rejected as INSUFFICIENT_STATIONS_FAULT)");
            return new Response(1, "Save and Modify success", null);
        }
        
        // INJECTED FAULT: Validate individual station name length (each must be between 2 and 50 characters)
        for (String station : stations) {
            if (station == null || station.trim().isEmpty()) {
                logger.warn("[createAndModifyRoute][INJECTED FAULT][INVALID_STATION_NAME_LENGTH_FAULT] station name is null or empty");
                FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                    true, 
                    "INVALID_STATION_NAME_LENGTH_FAULT", 
                    "Route creation rejected: station name cannot be null or empty"
                );
                return new Response<>(0, "Route creation rejected: station name cannot be null or empty", faultResponse);
            }
            
            if (station.trim().length() < 2 || station.trim().length() > 50) {
                logger.warn("[createAndModifyRoute][INJECTED FAULT][INVALID_STATION_NAME_LENGTH_FAULT] station name length invalid: {}", station.trim().length());
                FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                    true, 
                    "INVALID_STATION_NAME_LENGTH_FAULT", 
                    "Route creation rejected: each station name must be between 2 and 50 characters",
                    String.format("Station: '%s', Length: %d", station.trim(), station.trim().length())
                );
                return new Response<>(0, "Route creation rejected: each station name must be between 2 and 50 characters", faultResponse);
            }
        }
        
        // check stations
        String start = request.getStartStation();
        String end = request.getEndStation();
        if(!stations.contains(start) || !stations.contains(end)){
            logger.error("[createAndModifyRoute][check stations][start or end not included in stationList][start: {}, end: {}]", start, end);
            return new Response(0, "start or end station not include in stationList.", null);
        }

        Response response = checkStationsExists(stations, headers);
        if(response.getStatus() ==0) {
            return response;
        }

        HttpEntity requestEntity = new HttpEntity(request, null);
        String route_service_url = getServiceUrl("ts-route-service");
        ResponseEntity<Response<Route>> re = restTemplate.exchange(
                route_service_url + "/api/v1/routeservice/routes",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<Route>>() {
                });
        if (re.getStatusCode() != HttpStatus.ACCEPTED) {
            logger.error("[createAndModifyRoute][receive response][Get status error][response code: {}]", re.getStatusCodeValue());
        }
        return re.getBody();
    }

    @Override
    public Response deleteRoute(String routeId, HttpHeaders headers) {

        HttpEntity requestEntity = new HttpEntity(null);
        String route_service_url = getServiceUrl("ts-route-service");
        ResponseEntity<Response> re = restTemplate.exchange(
                route_service_url + "/api/v1/routeservice/routes/" + routeId,
                HttpMethod.DELETE,
                requestEntity,
                Response.class);
        if (re.getStatusCode() != HttpStatus.ACCEPTED) {
            logger.error("[deleteRoute][response response][Delete error][response code: {}]", re.getStatusCodeValue());
        }
        return re.getBody();

    }

    public Response checkStationsExists(List<String> stationNames, HttpHeaders headers) {
        logger.info("[checkStationsExists][Check Stations Exists][stationNames: {}]", stationNames);
        HttpEntity requestEntity = new HttpEntity(stationNames, null);
        String station_service_url=getServiceUrl("ts-station-service");
        ResponseEntity<Response> re = restTemplate.exchange(
                station_service_url + "/api/v1/stationservice/stations/idlist",
                HttpMethod.POST,
                requestEntity,
                Response.class);
        Response<Map<String, String>> r = re.getBody();
        if(r.getStatus() == 0) {
            return r;
        }
        Map<String, String> stationMap = r.getData();
        List<String> notExists = new ArrayList<>();
        for(Map.Entry<String, String> s : stationMap.entrySet()){
            if(s.getValue() == null ){
                // station not exist
                notExists.add(s.getKey());
            }
        }
        if(notExists.size() > 0) {
            return new Response<>(0, "some station not exists", notExists);
        }
        return new Response<>(1, "check stations Exist succeed", null);
    }
}