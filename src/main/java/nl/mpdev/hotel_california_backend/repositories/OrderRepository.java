package nl.mpdev.hotel_california_backend.repositories;

import nl.mpdev.hotel_california_backend.models.Order;
import nl.mpdev.hotel_california_backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
  Optional<Order> findOrderByOrderReference(String orderReference);
  List<Order> findAllOrdersByUser(User user);
}
