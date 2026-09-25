package com.restaurant.inventory;

import com.restaurant.inventory.model.Dish;
import com.restaurant.inventory.model.Ingredient;
import com.restaurant.inventory.model.Role;
import com.restaurant.inventory.model.User;
import com.restaurant.inventory.service.InventoryService;
import com.restaurant.inventory.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class RestaurantAppTest {

    private UserService userService;
    private InventoryService inventoryService;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        userService = UserService.getInstance();
        userService.resetUsers();
        inventoryService = InventoryService.getInstance();
        inventoryService.resetToDefaults();
    }

    @Test
    void testAuthenticationAndWarnings() {
        // 1. Empty username warning
        UserService.AuthResult emptyUser = userService.authenticate("", "password");
        assertFalse(emptyUser.success());
        assertTrue(emptyUser.message().contains("Username cannot be empty"));

        // 2. Empty password warning
        UserService.AuthResult emptyPass = userService.authenticate("admin", "");
        assertFalse(emptyPass.success());
        assertTrue(emptyPass.message().contains("Password cannot be empty"));

        // 3. Non-existent account warning
        UserService.AuthResult unknown = userService.authenticate("unknown_user", "pass123");
        assertFalse(unknown.success());
        assertTrue(unknown.message().contains("not found"));

        // 4. Incorrect password warning
        UserService.AuthResult wrongPass = userService.authenticate("admin", "wrong_password");
        assertFalse(wrongPass.success());
        assertTrue(wrongPass.message().contains("Incorrect password"));

        // 5. Successful Admin login
        UserService.AuthResult adminRes = userService.authenticate("admin", "admin123");
        assertTrue(adminRes.success());
        assertNotNull(adminRes.user());
        assertEquals(Role.ADMIN, adminRes.user().getRole());
        assertTrue(adminRes.user().isAdmin());

        // 6. Successful Normal User login
        UserService.AuthResult userRes = userService.authenticate("user", "user123");
        assertTrue(userRes.success());
        assertNotNull(userRes.user());
        assertEquals(Role.USER, userRes.user().getRole());
        assertFalse(userRes.user().isAdmin());
    }

    @Test
    void testProfileManagement() {
        User user = userService.findUser("user");
        assertNotNull(user);

        // Update personal information
        user.setFullName("Alex Updated");
        user.setEmail("alex.updated@test.com");
        user.setPhone("+1-555-9999");
        user.setCountry("United Kingdom");
        user.setDateOfBirth(LocalDate.of(1997, 5, 10));
        user.setGender("Male");
        user.setBio("Updated culinary lover profile");

        assertEquals("Alex Updated", user.getFullName());
        assertEquals("alex.updated@test.com", user.getEmail());
        assertEquals("+1-555-9999", user.getPhone());
        assertEquals("United Kingdom", user.getCountry());
        assertEquals(LocalDate.of(1997, 5, 10), user.getDateOfBirth());
        assertEquals("Updated culinary lover profile", user.getBio());

        // Password update
        user.setPassword("newsecret123");
        assertEquals("newsecret123", user.getPassword());
    }

    @Test
    void testMenuPictureManagement() {
        Dish burger = inventoryService.findDish("Cheeseburger");
        assertNotNull(burger);

        // Initially has picture
        assertTrue(burger.hasImage());
        assertEquals("burger.png", burger.getImageName());

        // Administrator changes picture
        burger.setImageName("pizza.png");
        assertEquals("pizza.png", burger.getImageName());

        // Administrator removes picture
        burger.removeImage();
        assertFalse(burger.hasImage());
        assertNull(burger.getImageName());

        // Administrator re-adds picture
        burger.setImageName("burger.png");
        assertTrue(burger.hasImage());
        assertEquals("burger.png", burger.getImageName());
    }

    @Test
    void testIngredientAndMenuWarningsAndPurchases() {
        Dish burger = inventoryService.findDish("Cheeseburger");
        assertNotNull(burger);
        Ingredient patty = inventoryService.findIngredient("Beef Patty");
        assertNotNull(patty);

        // Starting quantity of patty is 6
        assertEquals(6.0, patty.getQuantity());
        assertTrue(burger.isAvailable());

        // Place order for 6 burgers (uses all 6 patties)
        InventoryService.OrderResult orderResult = inventoryService.placeOrder(burger, 6);
        assertTrue(orderResult.success());
        assertEquals(0.0, patty.getQuantity());
        assertTrue(patty.isOutOfStock());

        // Warning requirement: Depleted ingredient is reported when it runs out
        assertFalse(orderResult.depletedIngredients().isEmpty());
        assertTrue(orderResult.depletedIngredients().contains("Beef Patty"));

        // Menu warning: Burger is now out of stock
        assertFalse(burger.isAvailable());
        List<String> missing = burger.missingIngredients(1);
        assertFalse(missing.isEmpty());
        assertTrue(missing.get(0).contains("Beef Patty"));

        // Ordering again must fail and trigger warning with missing ingredient details
        InventoryService.OrderResult failedOrder = inventoryService.placeOrder(burger, 1);
        assertFalse(failedOrder.success());
        assertTrue(failedOrder.message().contains("Not enough"));
        assertTrue(failedOrder.message().contains("Beef Patty"));

        // Check Items Needing Purchase on Admin Inventory page
        List<Ingredient> purchaseItems = inventoryService.getItemsNeedingPurchase();
        assertTrue(purchaseItems.stream().anyMatch(i -> i.getName().equalsIgnoreCase("Beef Patty")));

        // Admin restocks or auto-purchases deficits
        int restockedCount = inventoryService.purchaseAllDeficits();
        assertTrue(restockedCount > 0);
        assertFalse(patty.isOutOfStock());
        assertTrue(patty.getQuantity() > 0);

        // Burger is available again
        assertTrue(burger.isAvailable());
    }

    @Test
    void testAllFxmlViewsLoadSuccessfully() throws Exception {
        // Authenticate as Admin so all admin views load cleanly
        userService.authenticate("admin", "admin123");

        String[] fxmls = {
                "/fxml/LoginView.fxml",
                "/fxml/OrderView.fxml",
                "/fxml/InventoryView.fxml",
                "/fxml/StaffView.fxml",
                "/fxml/ToolsView.fxml",
                "/fxml/ProfileView.fxml",
                "/fxml/MainView.fxml"
        };

        for (String fxml : fxmls) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();

            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                    Object loaded = loader.load();
                    assertNotNull(loaded, "Failed to load root node from: " + fxml);
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out loading " + fxml);
            assertNull(error.get(), "FXML Error in " + fxml + ": " + (error.get() != null ? error.get().getMessage() : ""));
        }
    }
}
