import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}

// 2. Customer.java - Müşteri bilgileri ve ilişkileri
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
}

// 3. Product.java - Ürün bilgileri ve stok yönetimi
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {
    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private double currentPrice;

    @Column(nullable = false)
    private int stockQuantity;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceRecord> priceRecords = new ArrayList<>();

    public boolean isInStock(int requestedQuantity) {
        return this.stockQuantity >= requestedQuantity;
    }
}

// 4. Cart.java - Sepet ve ürün ilişkisi
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    private double totalAmount;

    public void recalculateTotal() {
        totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }
}

// 5. CartItem.java - Sepet içindeki ürün detayları
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitPrice;
}

// 6. Order.java - Sipariş ve müşteri ilişkisi
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String orderReference;

    @Column(nullable = false)
    private double totalPrice;

    public void calculateOrderTotal() {
        totalPrice = orderItems.stream()
                .mapToDouble(item -> item.getUnitPriceAtPurchase() * item.getQuantity())
                .sum();
    }
}

// 7. OrderItem.java - Siparişe ait ürün geçmiş fiyatı
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitPriceAtPurchase;
}

// 8. PriceRecord.java - Ürün fiyat geçmişi
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecord extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private LocalDateTime effectiveDate;
}

// CustomerService.java - Müşteri işlemleri
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public Customer createCustomer(String email, String fullName) {
        Customer customer = Customer.builder()
                .email(email)
                .fullName(fullName)
                .build();

        Cart cart = Cart.builder().customer(customer).build();
        customer.setCart(cart);

        return customerRepository.save(customer);
    }
}