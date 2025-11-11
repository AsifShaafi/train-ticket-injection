package adminorder.controller;

import edu.fudan.common.entity.*;
import edu.fudan.common.util.Response;
import adminorder.service.AdminOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.ResponseEntity.ok;

/**
 * @author fdse
 */
@RestController
@RequestMapping("/api/v1/adminorderservice")
public class AdminOrderController {

    @Autowired
    AdminOrderService adminOrderService;

    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    @GetMapping(path = "/welcome")
    public String home(@RequestHeader HttpHeaders headers) {
        return "Welcome to [Admin Order Service] !";
    }

    @CrossOrigin(origins = "*")
    @GetMapping(path = "/adminorder")
    public HttpEntity getAllOrders(@RequestHeader HttpHeaders headers) {
        logger.info("[getAllOrders][Get all orders][getAllOrders]");
        return ok(adminOrderService.getAllOrders(headers));
    }

    @PostMapping(value = "/adminorder")
    public HttpEntity addOrder(@RequestBody Order request, @RequestHeader HttpHeaders headers) {
        logger.info("[addOrder][Add new order][AccountID: {}]", request.getAccountId());
        Response response = adminOrderService.addOrder(request, headers);
        // Return HTTP 400 for injected faults
        if (response.getStatus() == 0 && response.getData() != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ok(response);
    }

    @PutMapping(value = "/adminorder")
    public HttpEntity updateOrder(@RequestBody Order request, @RequestHeader HttpHeaders headers) {
        logger.info("[updateOrder][Update order][AccountID: {}, OrderId: {}]", request.getAccountId(), request.getId());
        Response response = adminOrderService.updateOrder(request, headers);
        // Return HTTP 400 for injected faults
        if (response.getStatus() == 0 && response.getData() != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ok(response);
    }

    @DeleteMapping(value = "/adminorder/{orderId}/{trainNumber}")
    public HttpEntity deleteOrder(@PathVariable String orderId, @PathVariable String trainNumber, @RequestHeader HttpHeaders headers) {
        logger.info("[deleteOrder][Delete order][OrderId: {}, TrainNumber: {}]", orderId, trainNumber);
        Response response = adminOrderService.deleteOrder(orderId, trainNumber, headers);
        // Return HTTP 400 for injected faults
        if (response.getStatus() == 0 && response.getData() != null) {
            return ResponseEntity.badRequest().body(response);
        }
        return ok(response);
    }

}
