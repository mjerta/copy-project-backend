package nl.mpdev.hotel_california_backend.services;

import nl.mpdev.hotel_california_backend.dtos.orders.request.OrderCompleteRequestDto;
import nl.mpdev.hotel_california_backend.exceptions.GeneralException;
import nl.mpdev.hotel_california_backend.exceptions.RecordNotFoundException;
import nl.mpdev.hotel_california_backend.helpers.ServiceHelper;
import nl.mpdev.hotel_california_backend.models.*;
import nl.mpdev.hotel_california_backend.repositories.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final MealRepository mealRepository;
  private final DrinkRepository drinkRepository;
  private final LocationRepository locationRepository;
  private final UserRepository userRepository;
  private final ServiceHelper serviceHelper;

  public OrderService(OrderRepository orderRepository, MealRepository mealRepository, DrinkRepository drinkRepository,
                      LocationRepository locationRepository, UserRepository userRepository, ServiceHelper serviceHelper) {
    this.orderRepository = orderRepository;
    this.mealRepository = mealRepository;
    this.drinkRepository = drinkRepository;
    this.locationRepository = locationRepository;
    this.userRepository = userRepository;
    this.serviceHelper = serviceHelper;
  }

  public Order getOrderById(Integer id, String orderReference) {
    Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new RecordNotFoundException("No order is found"));
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Order validatedOrder = getOrderByUserOrOrderReference(orderReference, authentication, existingOrder);
    if (validatedOrder != null) return validatedOrder;
    throw new GeneralException("No order found with this user or reference: ");
  }

  public List<Order> getOrders() {
    return orderRepository.findAll();
  }

  public Order addOrder(Order entity) {
    if (entity.getMeals() == null && entity.getDrinks() == null) {
      throw new GeneralException("At least a drink or meal needs to be filled");
    }
    Order.OrderBuilder orderBuilder = Order.builder();
    createOrderIfUserLoggedIn(orderBuilder);
    if (entity.getMeals() != null) {
      orderBuilder.meals(entity.getMeals().stream()
        .map(meal -> mealRepository.findById(meal.getId())
          .orElseThrow(() -> new RecordNotFoundException("Meal not found")))
        .toList()
      );
    }
    if (entity.getDrinks() != null) {
      orderBuilder.drinks(entity.getDrinks().stream()
        .map(drink -> drinkRepository.findById(drink.getId())
          .orElseThrow(() -> new RecordNotFoundException("Drink not found")))
        .toList()
      );
    }
    if (entity.getDestination() != null) {
      orderBuilder.destination(locationRepository.findById(entity.getDestination().getId())
        .orElseThrow(() -> new RecordNotFoundException("Destination not found"))
      );
    }

    if (entity.getUser() != null) {
      orderBuilder.user(userRepository.findByUsername(entity.getUser().getUsername())
        .orElseThrow(() -> new RecordNotFoundException("User not found"))
      );
    }
    orderBuilder.orderDate(LocalDateTime.now());
    orderBuilder.orderReference(serviceHelper.generateOrderReference());

    entity = orderBuilder.build();

    return orderRepository.save(entity);
  }

  public Order updateOrder(Integer id, OrderCompleteRequestDto requestDto) {
    Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new RecordNotFoundException("Order not found"));
    verifyUserOrReference(existingOrder);
    Order.OrderBuilder orderBuilder = existingOrder.toBuilder();

    if (requestDto.getMeals() == null && requestDto.getDrinks() == null) {
      throw new GeneralException("At least a drink or meal needs to be filled");
    }
    if (requestDto.getMeals() != null) {
      orderBuilder.meals(requestDto.getMeals().stream()
        .map(meal -> mealRepository.findById(meal.getId())
          .orElseThrow(() -> new RecordNotFoundException("Meal not found")))
        .toList());
    }
    else orderBuilder.meals(null);

    if (requestDto.getDrinks() != null) {
      orderBuilder.drinks(requestDto.getDrinks().stream()
        .map(drink -> drinkRepository.findById(drink.getId())
          .orElseThrow(() -> new RecordNotFoundException("Drink not found")))
        .toList());
    }
    else orderBuilder.drinks(null);

    if (requestDto.getDestination() != null) {
      orderBuilder.destination(locationRepository.findById(requestDto.getDestination().getId())
        .orElseThrow(() -> new RecordNotFoundException("Destination not found")));
    }
    else orderBuilder.destination(null);
    orderBuilder.orderDate(LocalDateTime.now());

    return orderRepository.save(orderBuilder.build());
  }

  public Order updateOrderFields(Integer id, OrderCompleteRequestDto requestDto) {
    Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new RecordNotFoundException("Order not found"));
    verifyUserOrReference(existingOrder);
    Order.OrderBuilder orderBuilder = existingOrder.toBuilder();
    if (requestDto.getMeals() != null || requestDto.getMeals().getFirst().getId() != null) {
      orderBuilder.meals(requestDto.getMeals().stream()
        .map(meal -> mealRepository.findById(meal.getId()).orElseThrow(RecordNotFoundException::new))
        .toList());
    }
    if (requestDto.getDrinks() != null) {
      orderBuilder.drinks(requestDto.getDrinks().stream()
        .map(drink -> drinkRepository.findById(drink.getId())
          .orElseThrow(RecordNotFoundException::new))
        .toList());
    }
    if (requestDto.getStatus() != null) {
      orderBuilder.status(requestDto.getStatus());
    }
    if (requestDto.getDestination() != null) {
      orderBuilder.destination(locationRepository.findById(requestDto.getDestination().getId())
        .orElseThrow(RecordNotFoundException::new));
    }
    return orderRepository.save(orderBuilder.build());
  }

  // Perhaps I could be using something like this later as a helper method
  private <T, R> List<R> updateListOrKeepExisting(List<T> dtoList, List<R> existingList, Integer findId,
                                                  JpaRepository<R, Integer> repository) {
    if (dtoList != null) {
      return dtoList.stream()
        .map(dto -> repository.findById(findId).orElseThrow(RecordNotFoundException::new))
        .collect(Collectors.toList());
    }
    else {
      return existingList;
    }
  }

  public void deleteOrder(Integer id) {
    orderRepository.findById(id).orElseThrow(() -> new RecordNotFoundException("Order not found"));
    orderRepository.deleteById(id);
  }

  // Helper functions
  private void verifyUserOrReference(Order existingOrder) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
      if (existingOrder.getUser() == null) {
        throw new RecordNotFoundException("The order belongs to anomynous user");
      }
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      if (!existingOrder.getUser().getUsername().equals(userDetails.getUsername())) {
        throw new GeneralException("This order does not belong to the user");
      }
    }
  }

  private void createOrderIfUserLoggedIn(Order.OrderBuilder orderBuilder) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      orderBuilder.user(
        userRepository.findByUsername(userDetails.getUsername()).orElseThrow(() -> new RecordNotFoundException("No user is found")));
    }
  }

  private Order getOrderByUserOrOrderReference(String orderReference, Authentication authentication, Order existingOrder) {
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      User userToCheck = userRepository.findByUsername(userDetails.getUsername()).orElseThrow(RecordNotFoundException::new);
      if(existingOrder.getUser() == null) {
        throw new GeneralException("The order belongs to  a anomynous user");
      }
      if (existingOrder.getUser().getUsername().equals(userToCheck.getUsername())) {
        return existingOrder;
      }
    }
    if (orderReference.equals(existingOrder.getOrderReference())) {
      return existingOrder;
    }
    return null;
  }

}

