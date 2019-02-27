package me.whizvox.wsite.event;

import java.util.*;

public class EventManager {

  private Map<Class, Set<EventListener>> listeners;
  private Set<Object> events;

  public EventManager() {
    listeners = new HashMap<>();
    events = new HashSet<>();
  }

  public <T> void registerListener(EventListener<T> listener, Class<T> eventClass) {
    listeners.computeIfAbsent(eventClass, cls -> new HashSet<>()).add(listener);
  }

  public void dropListeners(Class<?> eventClass) {
    listeners.remove(eventClass);
  }

  public void dropAllListeners() {
    listeners.clear();
  }

  public synchronized void post(Object event) {
    events.add(event);
  }

  public void tick() {
    events.forEach(event -> Optional.ofNullable(listeners.getOrDefault(event.getClass(), null)).ifPresent(
        listeners -> listeners.forEach(listener -> listener.onEvent(event)))
    );
    events.clear();
  }

}
