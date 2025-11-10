package adminorder.service;

import adminorder.entity.FaultInjectionResponse;
import edu.fudan.common.entity.*;
import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author fdse
 */
@Service
public class AdminOrderServiceImpl implements AdminOrderService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private DiscoveryClient discoveryClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminOrderServiceImpl.class);

    private String getServiceUrl(String serviceName) {
        return "http://" + serviceName;
    }

    @Override
    public Response getAllOrders(HttpHeaders headers) {

        AdminOrderServiceImpl.LOGGER.info("[getAllOrders][Get All Orders: Generate Reponse Begin]");
        //Get all of the orders
        ArrayList<Order> orders = new ArrayList<>();
        //From ts-order-service
        HttpEntity requestEntity = new HttpEntity(null);

        String order_service_url = getServiceUrl("ts-order-service");
        ResponseEntity<Response<ArrayList<Order>>> re = restTemplate.exchange(
                order_service_url + "/api/v1/orderservice/order",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Response<ArrayList<Order>>>() {
                });
        Response<ArrayList<Order>> result = re.getBody();

        if (result.getStatus() == 1) {
            AdminOrderServiceImpl.LOGGER.info("[getAllOrders][Get Orders From ts-order-service successfully!]");
            ArrayList<Order> orders1 = result.getData();
            orders.addAll(orders1);
        } else {
            AdminOrderServiceImpl.LOGGER.error("[getAllOrders][receive response][Get Orders From ts-order-service fail!]");
        }
        //From ts-order-other-service
        HttpEntity requestEntity2 = new HttpEntity(null);
        String order_other_service_url = getServiceUrl("ts-order-other-service");
        ResponseEntity<Response<ArrayList<Order>>> re2 = restTemplate.exchange(
                order_other_service_url + "/api/v1/orderOtherService/orderOther",
                HttpMethod.GET,
                requestEntity2,
                new ParameterizedTypeReference<Response<ArrayList<Order>>>() {
                });
        result = re2.getBody();

        if (result.getStatus() == 1) {
            AdminOrderServiceImpl.LOGGER.info("[getAllOrders][Get Orders From ts-order-other-service successfully!]");
            ArrayList<Order> orders1 = (ArrayList<Order>) result.getData();
            orders.addAll(orders1);
        } else {
            AdminOrderServiceImpl.LOGGER.error("[getAllOrders][receive response][Get Orders From ts-order-other-service fail!]");
        }
        //Return orders
        return new Response<>(1, "Get the orders successfully!", orders);

    }

    @Override
    public Response deleteOrder(String orderId, String trainNumber, HttpHeaders headers) {
        Response deleteOrderResult;
        
        // INJECTED FAULT: Validate orderId length (must be between 10 and 50 characters)
        if (orderId == null || orderId.trim().isEmpty()) {
            AdminOrderServiceImpl.LOGGER.error("[deleteOrder][INJECTED FAULT][INVALID_ORDER_ID_LENGTH_FAULT] orderId is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_ORDER_ID_LENGTH_FAULT", 
                "Order deletion rejected: orderId cannot be null or empty"
            );
            return new Response<>(0, "Order deletion rejected: orderId cannot be null or empty", faultResponse);
        }
        
        // INJECTED FAULT: orderId length validation
        if (orderId.trim().length() < 10 || orderId.trim().length() > 50) {
            AdminOrderServiceImpl.LOGGER.error("[deleteOrder][INJECTED FAULT][INVALID_ORDER_ID_LENGTH_FAULT] orderId length invalid: {}, length: {}", 
                orderId, orderId.trim().length());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_ORDER_ID_LENGTH_FAULT", 
                "Order deletion rejected: orderId length must be between 10 and 50 characters",
                String.format("OrderID: '%s', Length: %d", orderId, orderId.trim().length())
            );
            return new Response<>(0, "Order deletion rejected: orderId length must be between 10 and 50 characters", faultResponse);
        }
        
        if (trainNumber.startsWith("G") || trainNumber.startsWith("D")) {
            AdminOrderServiceImpl.LOGGER.info("[deleteOrder][Delete Order][orderId: {}, trainNumber: {}]", orderId, trainNumber);
            HttpEntity requestEntity = new HttpEntity(null);
            String order_service_url = getServiceUrl("ts-order-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                     order_service_url + "/api/v1/orderservice/order/" + orderId,
                    HttpMethod.DELETE,
                    requestEntity,
                    Response.class);
            deleteOrderResult = re.getBody();

        } else {
            AdminOrderServiceImpl.LOGGER.info("[deleteOrder][Delete Order Other][trainNumber doesn't starts With G or D]");
            HttpEntity requestEntity = new HttpEntity(null);
            String order_other_service_url = getServiceUrl("ts-order-other-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                    order_other_service_url + "/api/v1/orderOtherService/orderOther/" + orderId,
                    HttpMethod.DELETE,
                    requestEntity,
                    Response.class);
            deleteOrderResult = re.getBody();

        }
        return deleteOrderResult;

    }

    @Override
    public Response updateOrder(Order request, HttpHeaders headers) {

        Response updateOrderResult;
        LOGGER.info("[updateOrder][UPDATE ORDER INFO][request info: {}]", request.toString());
        
        // INJECTED FAULT: Validate contactsName format
        if (request.getContactsName() == null || request.getContactsName().trim().isEmpty()) {
            LOGGER.error("[updateOrder][INJECTED FAULT][INVALID_CONTACTS_NAME_FAULT] contactsName is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_CONTACTS_NAME_FAULT", 
                "Order update rejected: contactsName cannot be null or empty"
            );
            return new Response<>(0, "Order update rejected: contactsName cannot be null or empty", faultResponse);
        }
        
        // INJECTED FAULT: contactsName cannot be purely numeric
        if (request.getContactsName().trim().matches("^\\d+$")) {
            LOGGER.error("[updateOrder][INJECTED FAULT][INVALID_CONTACTS_NAME_FAULT] contactsName is purely numeric: {}", request.getContactsName().trim());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_CONTACTS_NAME_FAULT", 
                "Order update rejected: contactsName cannot be purely numeric",
                request.getContactsName().trim()
            );
            return new Response<>(0, "Order update rejected: contactsName cannot be purely numeric", faultResponse);
        }
        
        // INJECTED FAULT: Validate seatNumber format (must be like "5A", "10B", etc.)
        if (request.getSeatNumber() == null || request.getSeatNumber().trim().isEmpty()) {
            LOGGER.error("[updateOrder][INJECTED FAULT][INVALID_SEAT_NUMBER_FAULT] seatNumber is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_SEAT_NUMBER_FAULT", 
                "Order update rejected: seatNumber cannot be null or empty"
            );
            return new Response<>(0, "Order update rejected: seatNumber cannot be null or empty", faultResponse);
        }
        
        // INJECTED FAULT: seatNumber must follow pattern like "1A", "10B" (digits followed by a letter)
        Pattern seatPattern = Pattern.compile("^\\d+[A-Z]$");
        if (!seatPattern.matcher(request.getSeatNumber().trim()).matches()) {
            LOGGER.error("[updateOrder][INJECTED FAULT][INVALID_SEAT_NUMBER_FAULT] seatNumber format invalid: {}", request.getSeatNumber());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_SEAT_NUMBER_FAULT", 
                "Order update rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)",
                request.getSeatNumber()
            );
            return new Response<>(0, "Order update rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)", faultResponse);
        }
        
        if (request.getTrainNumber().startsWith("G") || request.getTrainNumber().startsWith("D")) {

            AdminOrderServiceImpl.LOGGER.info("[updateOrder][Update Order][trainNumber starts With G or D]");
            HttpEntity requestEntity = new HttpEntity(request, headers);
            String order_service_url = getServiceUrl("ts-order-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                    order_service_url + "/api/v1/orderservice/order/admin",
                    HttpMethod.PUT,
                    requestEntity,
                    Response.class);
            updateOrderResult = re.getBody();

        } else {
            AdminOrderServiceImpl.LOGGER.info("[updateOrder][Add New Order Other][trainNumber doesn't starts With G or D]");
            HttpEntity requestEntity = new HttpEntity(request, headers);
            String order_other_service_url = getServiceUrl("ts-order-other-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                    order_other_service_url + "/api/v1/orderOtherService/orderOther/admin",
                    HttpMethod.PUT,
                    requestEntity,
                    Response.class);
            updateOrderResult = re.getBody();

        }
        return updateOrderResult;
    }

    @Override
    public Response addOrder(Order request, HttpHeaders headers) {

        Response addOrderResult;
        LOGGER.info("[addOrder][ADD ORDER][request info: {}]", request.toString());
        
        // INJECTED FAULT: Validate contactsName format
        if (request.getContactsName() == null || request.getContactsName().trim().isEmpty()) {
            LOGGER.error("[addOrder][INJECTED FAULT][INVALID_CONTACTS_NAME_FAULT] contactsName is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_CONTACTS_NAME_FAULT", 
                "Order rejected: contactsName cannot be null or empty"
            );
            return new Response<>(0, "Order rejected: contactsName cannot be null or empty", faultResponse);
        }
        
        // INJECTED FAULT: contactsName cannot be purely numeric
        if (request.getContactsName().trim().matches("^\\d+$")) {
            LOGGER.error("[addOrder][INJECTED FAULT][INVALID_CONTACTS_NAME_FAULT] contactsName is purely numeric: {}", request.getContactsName().trim());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_CONTACTS_NAME_FAULT", 
                "Order rejected: contactsName cannot be purely numeric",
                request.getContactsName().trim()
            );
            return new Response<>(0, "Order rejected: contactsName cannot be purely numeric", faultResponse);
        }
        
        // INJECTED FAULT: Validate seatNumber format (must be like "5A", "10B", etc.)
        if (request.getSeatNumber() == null || request.getSeatNumber().trim().isEmpty()) {
            LOGGER.error("[addOrder][INJECTED FAULT][INVALID_SEAT_NUMBER_FAULT] seatNumber is null or empty");
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_SEAT_NUMBER_FAULT", 
                "Order creation rejected: seatNumber cannot be null or empty"
            );
            return new Response<>(0, "Order creation rejected: seatNumber cannot be null or empty", faultResponse);
        }
        
        // INJECTED FAULT: seatNumber must follow pattern like "1A", "10B" (digits followed by a letter)
        Pattern seatPattern = Pattern.compile("^\\d+[A-Z]$");
        if (!seatPattern.matcher(request.getSeatNumber().trim()).matches()) {
            LOGGER.error("[addOrder][INJECTED FAULT][INVALID_SEAT_NUMBER_FAULT] seatNumber format invalid: {}", request.getSeatNumber());
            FaultInjectionResponse faultResponse = new FaultInjectionResponse(
                true, 
                "INVALID_SEAT_NUMBER_FAULT", 
                "Order creation rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)",
                request.getSeatNumber()
            );
            return new Response<>(0, "Order creation rejected: seatNumber must follow format like '5A' or '10B' (digits followed by uppercase letter)", faultResponse);
        }
        
        if (request.getTrainNumber().startsWith("G") || request.getTrainNumber().startsWith("D")) {
            AdminOrderServiceImpl.LOGGER.info("[addOrder][Add New Order][trainNumber starts With G or D]");
            HttpEntity requestEntity = new HttpEntity(request, headers);
            String order_service_url = getServiceUrl("ts-order-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                    order_service_url + "/api/v1/orderservice/order/admin",
                    HttpMethod.POST,
                    requestEntity,
                    Response.class);
            addOrderResult = re.getBody();

        } else {
            AdminOrderServiceImpl.LOGGER.info("[addOrder][Add New Order Other][trainNumber doesn't starts With G or D]");
            HttpEntity requestEntity = new HttpEntity(request, headers);
            String order_other_service_url = getServiceUrl("ts-order-other-service");
            ResponseEntity<Response> re = restTemplate.exchange(
                     order_other_service_url + "/api/v1/orderOtherService/orderOther/admin",
                    HttpMethod.POST,
                    requestEntity,
                    Response.class);
            addOrderResult = re.getBody();

        }
        return addOrderResult;

    }


}
