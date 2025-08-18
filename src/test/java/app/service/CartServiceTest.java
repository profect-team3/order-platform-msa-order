// package app.service;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;
//
// import java.util.*;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
//
// import app.domain.cart.model.dto.AddCartItemRequest;
// import app.domain.cart.model.dto.RedisCartItem;
// import app.domain.cart.model.entity.Cart;
// import app.domain.cart.model.entity.CartItem;
// import app.domain.cart.model.repository.CartItemRepository;
// import app.domain.cart.model.repository.CartRepository;
// import app.domain.cart.service.CartRedisService;
// import app.domain.cart.service.CartService;
// import app.global.apiPayload.code.status.ErrorStatus;
// import app.global.apiPayload.exception.GeneralException;
//
// @ExtendWith(MockitoExtension.class)
// class CartServiceTest {
//
//     @Mock
//     private CartRedisService cartRedisService;
//
//     @Mock
//     private CartRepository cartRepository;
//
//     @Mock
//     private CartItemRepository cartItemRepository;
//
//     @InjectMocks
//     private CartService cartService;
//
//     private Long userId;
//     private UUID menuId1;
//     private UUID menuId2;
//     private UUID storeId1;
//     private UUID storeId2;
//     private List<RedisCartItem> cartItems;
//
//     @BeforeEach
//     void setUp() {
//         userId = 1L;
//         menuId1 = UUID.randomUUID();
//         menuId2 = UUID.randomUUID();
//         storeId1 = UUID.randomUUID();
//         storeId2 = UUID.randomUUID();
//         cartItems = new ArrayList<>();
//     }
//
//     @Test
//     @DisplayName("장바구니에 새로운 아이템을 추가할 수 있다")
//     void addItem() {
//         AddCartItemRequest request = new AddCartItemRequest(menuId1, storeId1, 2);
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(true);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//         when(cartRedisService.saveCartToRedis(eq(userId), any())).thenReturn("성공");
//
//         cartService.addCartItem(userId, request);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), argThat(items ->
//             items.size() == 1 &&
//                 items.get(0).getMenuId().equals(menuId1) &&
//                 items.get(0).getStoreId().equals(storeId1) &&
//                 items.get(0).getQuantity() == 2
//         ));
//     }
//
//     @Test
//     @DisplayName("이미 존재하는 아이템을 추가하면 수량이 누적된다")
//     void addExistingItem() {
//         AddCartItemRequest request = new AddCartItemRequest(menuId1, storeId1, 2);
//         cartItems.add(RedisCartItem.builder().menuId(menuId1).storeId(storeId1).quantity(1).build());
//
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(true);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//         when(cartRedisService.saveCartToRedis(eq(userId), any())).thenReturn("성공");
//
//         cartService.addCartItem(userId, request);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), argThat(items ->
//             items.get(0).getMenuId().equals(menuId1) &&
//                 items.get(0).getStoreId().equals(storeId1) &&
//                 items.get(0).getQuantity() == 3
//         ));
//     }
//
//     @Test
//     @DisplayName("다른 매장의 아이템을 추가하면 기존 장바구니가 초기화된다")
//     void addDifferentStoreItem() {
//         AddCartItemRequest request = new AddCartItemRequest(menuId1, storeId1, 2);
//         cartItems.add(RedisCartItem.builder().menuId(menuId2).storeId(storeId2).quantity(1).build());
//
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(true);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//         when(cartRedisService.saveCartToRedis(eq(userId), any())).thenReturn("성공");
//
//         cartService.addCartItem(userId, request);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), argThat(items ->
//             items.size() == 1 &&
//                 items.get(0).getStoreId().equals(storeId1)
//         ));
//     }
//
//     @Test
//     @DisplayName("장바구니 아이템의 수량을 수정할 수 있다")
//     void updateItem() {
//         cartItems.add(RedisCartItem.builder().menuId(menuId1).storeId(storeId1).quantity(1).build());
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(true);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//         when(cartRedisService.saveCartToRedis(eq(userId), any())).thenReturn("성공");
//
//         cartService.updateCartItem(userId, menuId1, 5);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), argThat(items ->
//             items.get(0).getMenuId().equals(menuId1) &&
//                 items.get(0).getStoreId().equals(storeId1) &&
//                 items.get(0).getQuantity() == 5
//         ));
//     }
//
//     @Test
//     @DisplayName("장바구니에서 특정 아이템을 삭제할 수 있다")
//     void removeItem() {
//         when(cartRedisService.removeCartItem(userId, menuId1)).thenReturn("성공");
//
//         cartService.removeCartItem(userId, menuId1);
//
//         verify(cartRedisService).removeCartItem(userId, menuId1);
//     }
//
//     @Test
//     @DisplayName("Redis에 장바구니가 있으면 Redis에서 조회한다")
//     void getFromRedis() {
//         cartItems.add(RedisCartItem.builder().menuId(menuId1).storeId(storeId1).quantity(1).build());
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(true);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//
//         List<RedisCartItem> result = cartService.getCartFromCache(userId);
//
//         assertThat(result).isEqualTo(cartItems);
//         verify(cartRedisService, never()).saveCartToRedis(any(), any());
//     }
//
//     @Test
//     @DisplayName("Redis에 장바구니가 없으면 DB에서 로드한다")
//     void getFromDb() {
//         cartItems.add(RedisCartItem.builder().menuId(menuId1).storeId(storeId1).quantity(1).build());
//         when(cartRedisService.existsCartInRedis(userId)).thenReturn(false);
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(cartItems);
//
//         Cart cart = Cart.builder().cartId(UUID.randomUUID()).userId(userId).build();
//         CartItem cartItem = CartItem.builder()
//             .cart(cart)
//             .menuId(menuId1)
//             .storeId(storeId1)
//             .quantity(1)
//             .build();
//
//         when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
//         when(cartItemRepository.findByCart_CartId(cart.getCartId())).thenReturn(List.of(cartItem));
//
//         List<RedisCartItem> result = cartService.getCartFromCache(userId);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), any());
//         assertThat(result).isEqualTo(cartItems);
//     }
//
//     @Test
//     @DisplayName("장바구니의 모든 아이템을 삭제할 수 있다")
//     void clearItems() {
//         // given
//         when(cartRedisService.clearCartItems(userId)).thenReturn("cleared");
//
//         // when
//         String result = cartService.clearCartItems(userId);
//
//         // then
//         assertEquals("cleared", result);
//         verify(cartRedisService).clearCartItems(userId);
//     }
//
//     @Test
//     @DisplayName("DB의 장바구니 데이터를 Redis로 로드할 수 있다")
//     void loadDbToRedis() {
//         Cart cart = Cart.builder().cartId(UUID.randomUUID()).userId(userId).build();
//         CartItem cartItem = CartItem.builder()
//             .cart(cart)
//             .menuId(menuId1)
//             .storeId(storeId1)
//             .quantity(2)
//             .build();
//
//         when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
//         when(cartItemRepository.findByCart_CartId(cart.getCartId())).thenReturn(List.of(cartItem));
//         when(cartRedisService.saveCartToRedis(eq(userId), any())).thenReturn("성공");
//
//         cartService.loadDbToRedis(userId);
//
//         verify(cartRedisService).saveCartToRedis(eq(userId), argThat(items ->
//             items.size() == 1 &&
//                 items.get(0).getMenuId().equals(menuId1) &&
//                 items.get(0).getStoreId().equals(storeId1) &&
//                 items.get(0).getQuantity() == 2
//         ));
//     }
//
//     @Test
//     @DisplayName("Redis의 장바구니 데이터를 DB에 동기화할 수 있다")
//     void syncRedisToDb() {
//         RedisCartItem redisItem = RedisCartItem.builder().menuId(menuId1).storeId(storeId1).quantity(3).build();
//         Cart cart = Cart.builder().cartId(UUID.randomUUID()).userId(userId).build();
//
//         when(cartRedisService.getCartFromRedis(userId)).thenReturn(List.of(redisItem));
//         when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
//
//         cartService.syncRedisToDb(userId);
//
//         verify(cartItemRepository).deleteByCart_CartId(cart.getCartId());
//         verify(cartItemRepository).saveAll(argThat((List<CartItem> items) ->
//             items.size() == 1 &&
//                 items.get(0).getMenuId().equals(menuId1) &&
//                 items.get(0).getStoreId().equals(storeId1) &&
//                 items.get(0).getQuantity() == 3
//         ));
//     }
// }