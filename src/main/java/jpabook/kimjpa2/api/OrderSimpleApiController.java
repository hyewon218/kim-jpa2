package jpabook.kimjpa2.api;

import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.List;
import jpabook.kimjpa2.domain.Address;
import jpabook.kimjpa2.domain.Order;
import jpabook.kimjpa2.domain.OrderStatus;
import jpabook.kimjpa2.repository.OrderRepository;
import jpabook.kimjpa2.repository.OrderSearch;
import jpabook.kimjpa2.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.kimjpa2.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * xToOne(ManyToOne, OneToOne) 관계 최적화
 * Order
 * Order -> Member(ManyToOne)
 * Order -> Delivery(OneToOne)
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository; // 의존관계 주입

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            // FORCE_LAZY_LOADING 옵션 끄고 원하는 것만 골라서 출력하는 방법
            // order.getMember() -> 여기까지 프록시 객체(DB에 쿼리가 안 날아감)
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO 로 변환(fetch join 사용X)
     * - 단점: 지연로딩으로 쿼리 N번 호출
     */
    @GetMapping("/api/v2/simple-orders")
    public List<OrderSimpleDto> ordersV2() {
        List<Order> orders = orderRepository.findAll();
        List<OrderSimpleDto> result = orders.stream()
                .map(OrderSimpleDto::new)
                .collect(toList());
        return result;
    }

    /**
     * V3. 엔티티를 조회해서 DTO 로 변환(fetch join 사용O)
     * - fetch join 으로 쿼리 1번 호출
     * 참고: fetch join 에 대한 자세한 내용은 JPA 기본편 참고(정말 중요함)
     */
    @GetMapping("/api/v3/simple-orders")
    public List<OrderSimpleDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<OrderSimpleDto> result = orders.stream()
                .map(OrderSimpleDto::new)
                .collect(toList());
        return result;
    }

    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class OrderSimpleDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate; // 주문시간
        private OrderStatus orderStatus;
        private Address address;

        public OrderSimpleDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();// LAZY 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();// LAZY 초기화
        }
    }
}
