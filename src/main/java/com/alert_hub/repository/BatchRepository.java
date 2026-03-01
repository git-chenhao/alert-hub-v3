package com.alert_hub.repository;

import com.alert_hub.entity.AlertBatch;
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
 * 告警批次数据访问层
 */
@Repository
public interface BatchRepository extends JpaRepository<AlertBatch, Long> {

    /**
     * 根据分组键和状态查询批次
     *
     * @param groupKey 分组键
     * @param status 状态
     * @return 批次对象
     */
    Optional<AlertBatch> findByGroupKeyAndStatus(String groupKey, String status);

    /**
     * 查询指定状态的批次
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 批次列表
     */
    Page<AlertBatch> findByStatus(String status, Pageable pageable);

    /**
     * 查询所有待处理的批次
     *
     * @return 批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'PENDING' ORDER BY b.createdAt ASC")
    List<AlertBatch> findPendingBatches();

    /**
     * 查询需要触发聚合的批次（达到等待时间或最大数量）
     *
     * @param maxWaitSeconds 最大等待秒数
     * @param maxCount 最大告警数
     * @return 批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'PENDING' " +
           "AND (b.alertCount >= :maxCount OR b.triggeredAt <= :triggerBefore)")
    List<AlertBatch> findTriggerableBatches(
            @Param("triggerBefore") LocalDateTime triggerBefore,
            @Param("maxCount") int maxCount);

    /**
     * 更新批次状态
     *
     * @param id 批次 ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.status = :newStatus, b.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE b.id = :id AND b.status = :oldStatus")
    int updateStatus(@Param("id") Long id,
                     @Param("oldStatus") String oldStatus,
                     @Param("newStatus") String newStatus);

    /**
     * 标记批次为处理中
     *
     * @param id 批次 ID
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.status = 'PROCESSING', b.processingAt = CURRENT_TIMESTAMP, " +
           "b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id AND b.status = 'PENDING'")
    int markAsProcessing(@Param("id") Long id);

    /**
     * 标记批次为已完成
     *
     * @param id 批次 ID
     * @param analysisSummary 分析摘要
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.status = 'COMPLETED', b.analysisSummary = :analysisSummary, " +
           "b.completedAt = CURRENT_TIMESTAMP, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    int markAsCompleted(@Param("id") Long id, @Param("analysisSummary") String analysisSummary);

    /**
     * 标记批次为失败
     *
     * @param id 批次 ID
     * @param errorMessage 错误信息
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.status = 'FAILED', b.errorMessage = :errorMessage, " +
           "b.completedAt = CURRENT_TIMESTAMP, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id")
    int markAsFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    /**
     * 增加批次告警计数
     *
     * @param id 批次 ID
     * @param count 增加数量
     * @return 更新数量
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.alertCount = b.alertCount + :count, b.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE b.id = :id")
    int incrementAlertCount(@Param("id") Long id, @Param("count") int count);

    /**
     * 统计指定状态的批次数量
     *
     * @param status 状态
     * @return 数量
     */
    long countByStatus(String status);

    /**
     * 查询指定时间范围内的批次
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 批次列表
     */
    Page<AlertBatch> findByCreatedAtBetween(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    /**
     * 删除指定时间之前的批次（数据清理）
     *
     * @param before 时间点
     * @return 删除数量
     */
    @Modifying
    @Query("DELETE FROM AlertBatch b WHERE b.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);

}
