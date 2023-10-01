package jpabook.kimjpa2.api;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import jpabook.kimjpa2.domain.Address;
import jpabook.kimjpa2.domain.Order;
import jpabook.kimjpa2.domain.OrderItem;
import jpabook.kimjpa2.domain.OrderStatus;
import jpabook.kimjpa2.repository.OrderRepository;
import jpabook.kimjpa2.repository.order.query.OrderFlatDto;
import jpabook.kimjpa2.repository.order.query.OrderItemQueryDto;
import jpabook.kimjpa2.repository.order.query.OrderQueryDto;
import jpabook.kimjpa2.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll();
        for (Order order : all) {
            order.getMember().getName();// Lazy 강제 초기화
            order.getDelivery().getAddress();// Lazy 강제 초기화
            // 주문 상품 정보 추가
            // 주문과 관련된 orderItems 를 다 가져와 돌리면서 이름을 가져온다.
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());// Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO 로 변환(fetch join 사용X)
     * - 트랜잭션 안에서 지연 로딩 필요
     * - 단점: 지연로딩으로 쿼리 N번 호출
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());

        return result;
    }

    /**
     * V3. 엔티티를 조회해서 DTO 로 변환(fetch join 사용O)
     * - fetch join 으로 쿼리 1번 호출
     * - 단점 : 페이징 불가능
     * - 페이징 시에는 N 부분을 포기해야 함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경 가능)
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());

        return result;
    }

    /**
     * V3.1 엔티티를 조회해서 DTO 로 변환 페이징 고려
     * - ToOne 관계만 우선 모두 페치 조인으로 최적화
     * - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize 로 최적화
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                        @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());

        return result;
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
            .collect(groupingBy(
                o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(),
                    o.getOrderStatus(), o.getAddress()),
                mapping(
                    o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(),
                        o.getCount()), toList())
            )).entrySet().stream()
            .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(),
                e.getKey().getOrderDate(), e.getKey().getOrderStatus(), e.getKey().getAddress(),
                e.getValue()))
            .collect(toList());
    }

    @Data
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate; // 주문시간
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems; // OrderItem 엔티티가 외부로 노출되면 안 되기 때문에 Dto 로

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(toList());
        }
    }

    @Data
    static class OrderItemDto {

        private String itemName;// 상품 명
        private int orderPrice; // 주문 가격
        private int count;      // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
