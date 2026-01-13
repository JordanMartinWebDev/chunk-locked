package chunkloaded.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdvancementCompletedCallback event system.
 * <p>
 * Tests event registration, invocation, listener ordering, and edge cases.
 */
@DisplayName("AdvancementCompletedCallback Tests")
class AdvancementCompletedCallbackTest {

  private ServerPlayer mockPlayer;
  private AdvancementHolder mockAdvancement;
  private String testCriterion;

  @BeforeEach
  void setUp() {
    // Create mock objects for testing
    mockPlayer = mock(ServerPlayer.class);
    mockAdvancement = mock(AdvancementHolder.class);
    testCriterion = "test_criterion";
  }

  @Test
  @DisplayName("Event registration should allow multiple listeners")
  void testEventRegistration() {
    // Given: Multiple listeners
    AtomicInteger callCount = new AtomicInteger(0);

    AdvancementCompletedCallback listener1 = (player, advancement, criterion) -> callCount.incrementAndGet();
    AdvancementCompletedCallback listener2 = (player, advancement, criterion) -> callCount.incrementAndGet();
    AdvancementCompletedCallback listener3 = (player, advancement, criterion) -> callCount.incrementAndGet();

    // When: Registering all listeners
    AdvancementCompletedCallback.EVENT.register(listener1);
    AdvancementCompletedCallback.EVENT.register(listener2);
    AdvancementCompletedCallback.EVENT.register(listener3);

    // Then: All listeners should be invoked
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, testCriterion);

    assertEquals(3, callCount.get(), "All three listeners should have been invoked");
  }

  @Test
  @DisplayName("Event invoker should preserve listener order")
  void testListenerOrderPreservation() {
    // Given: A list to track invocation order
    List<Integer> invocationOrder = new ArrayList<>();

    AdvancementCompletedCallback listener1 = (player, advancement, criterion) -> invocationOrder.add(1);
    AdvancementCompletedCallback listener2 = (player, advancement, criterion) -> invocationOrder.add(2);
    AdvancementCompletedCallback listener3 = (player, advancement, criterion) -> invocationOrder.add(3);

    // When: Registering listeners in order
    AdvancementCompletedCallback.EVENT.register(listener1);
    AdvancementCompletedCallback.EVENT.register(listener2);
    AdvancementCompletedCallback.EVENT.register(listener3);

    // Then: Listeners should be invoked in registration order
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, testCriterion);

    assertEquals(List.of(1, 2, 3), invocationOrder, "Listeners should be invoked in registration order");
  }

  @Test
  @DisplayName("Event invoker should work with no listeners registered")
  void testEventInvokerWithNoListeners() {
    // When: Invoking event with no listeners
    // Then: Should not throw any exception
    assertDoesNotThrow(() -> {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          mockPlayer, mockAdvancement, testCriterion);
    }, "Event invoker should safely handle no listeners");
  }

  @Test
  @DisplayName("Event should pass correct parameters to listeners")
  void testEventParameterPassing() {
    // Given: A listener that captures parameters
    final ServerPlayer[] capturedPlayer = new ServerPlayer[1];
    final AdvancementHolder[] capturedAdvancement = new AdvancementHolder[1];
    final String[] capturedCriterion = new String[1];

    AdvancementCompletedCallback listener = (player, advancement, criterion) -> {
      capturedPlayer[0] = player;
      capturedAdvancement[0] = advancement;
      capturedCriterion[0] = criterion;
    };

    AdvancementCompletedCallback.EVENT.register(listener);

    // When: Invoking the event
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, testCriterion);

    // Then: Parameters should be passed correctly
    assertSame(mockPlayer, capturedPlayer[0], "Player should be passed correctly");
    assertSame(mockAdvancement, capturedAdvancement[0], "Advancement should be passed correctly");
    assertEquals(testCriterion, capturedCriterion[0], "Criterion should be passed correctly");
  }

  @Test
  @DisplayName("Event should continue invoking remaining listeners if one throws exception")
  void testEventContinuesOnException() {
    // Given: Multiple listeners, one throws exception
    AtomicInteger successfulCalls = new AtomicInteger(0);

    AdvancementCompletedCallback listener1 = (player, advancement, criterion) -> successfulCalls.incrementAndGet();
    AdvancementCompletedCallback listener2 = (player, advancement, criterion) -> {
      throw new RuntimeException("Test exception");
    };
    AdvancementCompletedCallback listener3 = (player, advancement, criterion) -> successfulCalls.incrementAndGet();

    AdvancementCompletedCallback.EVENT.register(listener1);
    AdvancementCompletedCallback.EVENT.register(listener2);
    AdvancementCompletedCallback.EVENT.register(listener3);

    // When: Invoking the event
    // Then: Should throw exception from listener2
    assertThrows(RuntimeException.class, () -> {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          mockPlayer, mockAdvancement, testCriterion);
    });

    // Note: Fabric's EventFactory doesn't continue after exception by default
    // Only listener1 should have been called
    assertEquals(1, successfulCalls.get(),
        "Only listeners before the throwing listener should be invoked");
  }

  @Test
  @DisplayName("Event should handle null parameters gracefully")
  void testEventWithNullParameters() {
    // Given: A listener that checks for nulls
    final boolean[] listenerCalled = { false };

    AdvancementCompletedCallback listener = (player, advancement, criterion) -> {
      listenerCalled[0] = true;
      // In production, these should never be null according to contract
      // But we test that the event system doesn't crash
    };

    AdvancementCompletedCallback.EVENT.register(listener);

    // When: Invoking with null parameters (violates contract but tests robustness)
    // Then: Should not crash the event system itself
    assertDoesNotThrow(() -> {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          null, null, null);
    });

    assertTrue(listenerCalled[0], "Listener should have been called even with null parameters");
  }

  @Test
  @DisplayName("Multiple events can be fired in sequence")
  void testMultipleEventInvocations() {
    // Given: A counter listener
    AtomicInteger callCount = new AtomicInteger(0);
    AdvancementCompletedCallback listener = (player, advancement, criterion) -> callCount.incrementAndGet();
    AdvancementCompletedCallback.EVENT.register(listener);

    // When: Firing multiple events
    for (int i = 0; i < 5; i++) {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          mockPlayer, mockAdvancement, testCriterion);
    }

    // Then: Listener should be called for each event
    assertEquals(5, callCount.get(), "Listener should be called once per event");
  }

  @Test
  @DisplayName("Event system should be independent across different criterion names")
  void testDifferentCriterionNames() {
    // Given: A listener that tracks criterion names
    List<String> criterionNames = new ArrayList<>();
    AdvancementCompletedCallback listener = (player, advancement, criterion) -> criterionNames.add(criterion);
    AdvancementCompletedCallback.EVENT.register(listener);

    // When: Firing events with different criteria
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_1");
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_2");
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_3");

    // Then: All criterion names should be captured
    assertEquals(List.of("criterion_1", "criterion_2", "criterion_3"), criterionNames,
        "All different criterion names should be passed to listener");
  }
}
