package com.alert_hub.repository;

import com.alert_hub.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警数据访问层
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 根据指纹查询告警
     *
     * @param fingerprint 告警指纹
     * @return 告警对象
     */
    Optional<Alert> findByFingerprint(String fingerprint);

    /**
     * 检查指纹是否存在于指定时间窗口内（用于去重）
     *
     * @param fingerprint 告警指纹
     * @param startTime 开始时间
     * @return 是否存在
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Alert a " +
           "WHERE a.fingerprint = :fingerprint AND a.createdAt >= :startTime")
    boolean existsByFingerprintAndCreatedAtAfter(
            @Param("fingerprint") String fingerprint,
            @Param("startTime") LocalDateTime startTime);

    /**
     * 根据状态查询告警
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<Alert> findByStatus(String status, Pageable pageable);

    /**
     * 根据来源查询告警
     *
     * @param source 来源
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<Alert> findBySource(String source, Pageable pageable);

    /**
     * 根据批次 ID 查询告警
     *
     * @param batchId 批次 ID
     * @return 告警列表
     */
    List<Alert> findByBatchId(Long batchId);

    /**
     * 查询未分配批次的告警
     *
     * @return 告警列表
     */
    @Query("SELECT a FROM Alert a WHERE a.batchId IS NULL AND a.status = 'pending'")
    List<Alert> findUnbatchedAlerts();

    /**
     * 批量更新告警的批次 ID
     *
     * @param ids 告警 ID 列表
     * @param batchId 批次 ID
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE Alert a SET a.batchId = :batchId, a.status = 'batched', a.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE a.id IN :ids")
    int updateBatchIdByIds(@Param("ids") List<Long> ids, @Param("batchId") Long batchId);

    /**
     * 统计指定状态的告警数量
     *
     * @param status 状态
     * @return 数量
     */
    long countByStatus(String status);

    /**
     * 查询指定时间范围内的告警
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 告警列表
     */
    Page<Alert> findByCreatedAtBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    /**
     * 删除指定时间之前的告警（数据清理）
     *
     * @param before 时间点
     * @return 删除数量
     */
    @Modifying
    @Query("DELETE FROM Alert a WHERE a.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);

}
