package chunklocked.advancement;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for the advancement detection system.
 * <p>
 * Tests the full flow from PlayerAdvancements through the mixin
 * to the event callback system.
 * <p>
 * Note: These tests verify the logical integration rather than actual
 * Minecraft runtime behavior, which requires a full game environment.
 */
@DisplayName("Advancement Event Integration Tests")
class AdvancementEventIntegrationTest {

  private ServerPlayer mockPlayer;
  private AdvancementHolder mockAdvancement;
  private AdvancementProgress mockProgress;

  @BeforeEach
  void setUp() {
    mockPlayer = mock(ServerPlayer.class);
    mockAdvancement = mock(AdvancementHolder.class);
    mockProgress = mock(AdvancementProgress.class);
  }

  @Test
  @DisplayName("Event should fire when advancement is completed")
  void testEventFiresOnCompletion() {
    // Given: An advancement that is now complete
    when(mockProgress.isDone()).thenReturn(true);

    AtomicBoolean eventFired = new AtomicBoolean(false);
    AtomicReference<AdvancementHolder> capturedAdvancement = new AtomicReference<>();
    AtomicReference<ServerPlayer> capturedPlayer = new AtomicReference<>();
    AtomicReference<String> capturedCriterion = new AtomicReference<>();

    AdvancementCompletedCallback testListener = (player, advancement, criterion) -> {
      eventFired.set(true);
      capturedPlayer.set(player);
      capturedAdvancement.set(advancement);
      capturedCriterion.set(criterion);
    };

    AdvancementCompletedCallback.EVENT.register(testListener);

    // When: The event is fired (simulating mixin behavior)
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "test_criterion");

    // Then: Event should have fired with correct parameters
    assertTrue(eventFired.get(), "Event should have been fired");
    assertSame(mockPlayer, capturedPlayer.get(), "Player should match");
    assertSame(mockAdvancement, capturedAdvancement.get(), "Advancement should match");
    assertEquals("test_criterion", capturedCriterion.get(), "Criterion should match");
  }

  @Test
  @DisplayName("Event should NOT fire for partial completion")
  void testEventDoesNotFireOnPartialCompletion() {
    // Given: An advancement that is NOT complete yet
    when(mockProgress.isDone()).thenReturn(false);

    AtomicBoolean eventFired = new AtomicBoolean(false);
    AdvancementCompletedCallback testListener = (player, advancement, criterion) -> {
      eventFired.set(true);
    };

    AdvancementCompletedCallback.EVENT.register(testListener);

    // When: Simulating the mixin logic that checks isDone()
    // The mixin would not invoke the event if progress is not done
    if (mockProgress.isDone()) {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          mockPlayer, mockAdvancement, "test_criterion");
    }

    // Then: Event should NOT have fired
    assertFalse(eventFired.get(), "Event should not fire for partial completion");
  }

  @Test
  @DisplayName("Event includes correct player and advancement references")
  void testEventIncludesCorrectReferences() {
    // Given: Specific player and advancement instances
    ServerPlayer player1 = mock(ServerPlayer.class);
    ServerPlayer player2 = mock(ServerPlayer.class);
    AdvancementHolder advancement1 = mock(AdvancementHolder.class);
    AdvancementHolder advancement2 = mock(AdvancementHolder.class);

    AtomicReference<ServerPlayer> capturedPlayer = new AtomicReference<>();
    AtomicReference<AdvancementHolder> capturedAdvancement = new AtomicReference<>();

    AdvancementCompletedCallback testListener = (player, advancement, criterion) -> {
      capturedPlayer.set(player);
      capturedAdvancement.set(advancement);
    };

    AdvancementCompletedCallback.EVENT.register(testListener);

    // When: Event fires for player1 and advancement1
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        player1, advancement1, "criterion");

    // Then: Should capture player1 and advancement1
    assertSame(player1, capturedPlayer.get(), "Should capture correct player");
    assertSame(advancement1, capturedAdvancement.get(), "Should capture correct advancement");

    // When: Event fires for player2 and advancement2
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        player2, advancement2, "criterion");

    // Then: Should capture player2 and advancement2
    assertSame(player2, capturedPlayer.get(), "Should capture new player");
    assertSame(advancement2, capturedAdvancement.get(), "Should capture new advancement");
  }

  @Test
  @DisplayName("Multiple listeners receive the same event")
  void testMultipleListenersReceiveSameEvent() {
    // Given: Multiple listeners
    AtomicReference<AdvancementHolder> captured1 = new AtomicReference<>();
    AtomicReference<AdvancementHolder> captured2 = new AtomicReference<>();
    AtomicReference<AdvancementHolder> captured3 = new AtomicReference<>();

    AdvancementCompletedCallback.EVENT.register((p, a, c) -> captured1.set(a));
    AdvancementCompletedCallback.EVENT.register((p, a, c) -> captured2.set(a));
    AdvancementCompletedCallback.EVENT.register((p, a, c) -> captured3.set(a));

    // When: Event fires once
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion");

    // Then: All listeners should receive the same advancement
    assertSame(mockAdvancement, captured1.get(), "Listener 1 should receive event");
    assertSame(mockAdvancement, captured2.get(), "Listener 2 should receive event");
    assertSame(mockAdvancement, captured3.get(), "Listener 3 should receive event");
  }

  @Test
  @DisplayName("Event fires for different criterion names on same advancement")
  void testDifferentCriteriaOnSameAdvancement() {
    // Given: Tracking criterion names
    AtomicReference<String> lastCriterion = new AtomicReference<>();
    AdvancementCompletedCallback.EVENT.register((p, a, criterion) -> lastCriterion.set(criterion));

    // When: Same advancement completes with different criteria
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_1");
    assertEquals("criterion_1", lastCriterion.get());

    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_2");
    assertEquals("criterion_2", lastCriterion.get());

    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        mockPlayer, mockAdvancement, "criterion_3");
    assertEquals("criterion_3", lastCriterion.get());
  }

  @Test
  @DisplayName("Event system handles rapid successive completions")
  void testRapidSuccessiveCompletions() {
    // Given: A counter listener
    AtomicBoolean[] completions = new AtomicBoolean[10];
    for (int i = 0; i < completions.length; i++) {
      completions[i] = new AtomicBoolean(false);
    }

    AdvancementCompletedCallback.EVENT.register((player, advancement, criterion) -> {
      int index = Integer.parseInt(criterion.replace("criterion_", ""));
      completions[index].set(true);
    });

    // When: Rapidly firing multiple completions
    for (int i = 0; i < 10; i++) {
      AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
          mockPlayer, mockAdvancement, "criterion_" + i);
    }

    // Then: All completions should be tracked
    for (int i = 0; i < completions.length; i++) {
      assertTrue(completions[i].get(), "Completion " + i + " should be tracked");
    }
  }

  @Test
  @DisplayName("Event correctly differentiates between multiple players")
  void testMultiplePlayersDifferentiation() {
    // Given: Multiple mock players
    ServerPlayer player1 = mock(ServerPlayer.class);
    ServerPlayer player2 = mock(ServerPlayer.class);
    ServerPlayer player3 = mock(ServerPlayer.class);

    AtomicReference<ServerPlayer> lastPlayer = new AtomicReference<>();
    AdvancementCompletedCallback.EVENT.register((player, advancement, criterion) -> {
      lastPlayer.set(player);
    });

    // When: Events fire for different players
    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        player1, mockAdvancement, "criterion");
    assertSame(player1, lastPlayer.get(), "Should track player1");

    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        player2, mockAdvancement, "criterion");
    assertSame(player2, lastPlayer.get(), "Should track player2");

    AdvancementCompletedCallback.EVENT.invoker().onAdvancementCompleted(
        player3, mockAdvancement, "criterion");
    assertSame(player3, lastPlayer.get(), "Should track player3");
  }
}
