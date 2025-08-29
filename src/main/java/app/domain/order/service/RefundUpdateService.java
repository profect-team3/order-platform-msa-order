package app.domain.order.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.commonUtil.apiPayload.code.status.ErrorStatus;
import app.commonUtil.apiPayload.exception.GeneralException;
import app.domain.order.model.entity.Orders;
import app.domain.order.model.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundUpdateService {

	private final OrdersRepository ordersRepository;

	@Transactional
	public void updateRefundableStatus(UUID orderId) {
		Orders order = ordersRepository.findById(orderId)
			.orElseThrow(() -> new GeneralException(ErrorStatus.ORDER_NOT_FOUND));
		order.disableRefund();
		ordersRepository.save(order);
	}
}