package app.domain.order.model.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.domain.order.model.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, UUID> {
	Page<Orders> findAllByUserIdAndDeliveryAddressIsNotNull(Long userId, Pageable pageable);

	List<Orders> findByUserId(Long userId);

	List<Orders> findByStoreId(UUID storeId);
}
