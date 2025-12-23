package com.bitirme.repository;

import com.bitirme.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SourceRepository extends JpaRepository<Source, Integer> {
    Optional<Source> findByName(String name);
    boolean existsByName(String name);
    List<Source> findByActiveTrue();
}


