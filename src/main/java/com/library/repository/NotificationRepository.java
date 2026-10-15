package com.library.repository;

import com.library.model.Notification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdOrderByIdDesc(Long userId);

    List<Notification> findTop5ByUserIdOrderByIdDesc(Long userId);

    List<Notification> findTop10ByUserIdAndSeenFalseOrderByIdDesc(Long userId);

    List<Notification> findTop15ByOrderByIdDesc();

    long countByUserIdAndSeenFalse(Long userId);

    boolean existsByUserIdAndMessageAndCreatedAtAfter(Long userId, String message, LocalDateTime after);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.seen = true where n.user.id = :userId")
    int markAllSeen(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
