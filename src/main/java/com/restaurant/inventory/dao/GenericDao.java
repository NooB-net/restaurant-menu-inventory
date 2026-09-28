package com.restaurant.inventory.dao;

import java.util.List;
import java.util.Optional;

/**
 * Advanced OOP Concept: Generic Interface
 * Defines standard CRUD operations for data access objects.
 *
 * @param <T> Entity type
 * @param <ID> Entity identifier type
 */
public interface GenericDao<T, ID> {
    void create(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void update(T entity);
    void delete(ID id);
}
