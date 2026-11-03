package com.mathbuddy.repository;

import com.mathbuddy.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChildRepository extends JpaRepository<Child, Long> {

    List<Child> findAllByOrderByCreatedAtDesc();
}

