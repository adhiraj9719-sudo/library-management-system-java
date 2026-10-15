package com.library.repository;

import com.library.model.SearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    List<SearchHistory> findTop8ByUserIdOrderByIdDesc(Long userId);

    void deleteByUserId(Long userId);
}
