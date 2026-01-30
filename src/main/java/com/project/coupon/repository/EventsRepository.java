package com.project.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.coupon.entity.Events;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {
}
