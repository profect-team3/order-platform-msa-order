package app.domain.cart.internal;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.commonSecurity.TokenPrincipalParser;
import app.domain.cart.model.entity.Cart;
import app.domain.cart.model.repository.CartRepository;
import app.global.apiPayload.code.status.ErrorStatus;
import app.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InternalCartService {

	private final CartRepository cartRepository;
	private final TokenPrincipalParser tokenPrincipalParser;

	@Transactional
	public String createCart(Authentication authentication) {
		try {
			String userIdStr = tokenPrincipalParser.getUserId(authentication);
			Long userId = Long.parseLong(userIdStr);
			Cart cart = Cart.builder()
				.userId(userId)
				.build();
			cartRepository.save(cart);
			return "장바구니가 생성되었습니다.";
		} catch (Exception e) {
			throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
		}
	}
}
