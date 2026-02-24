package com.beyond.pochaon.customerTable.domain;

import com.beyond.pochaon.common.BaseTimeEntity;
import com.beyond.pochaon.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class CustomerTable extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerTableId;

    @Column(nullable = false)
    private int tableNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Store store;

    //    groupId : 새로운 손님이 오면 발급될 uuid
    //테이블당 손님그룹 구분하기 위한 uuid
    @Column(unique = true, columnDefinition = "BINARY(16)")
    private UUID groupId;

    //    테이블 상태 (standby:대기중 , using :사용중 )
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TableStatus tableStatus = TableStatus.STANDBY;


    public void setTableStatusUsing() {
        this.tableStatus = TableStatus.USING;
    }

    public void setTableStatusStandBy() {
        this.tableStatus = TableStatus.STANDBY;
    }

    public void clearTable() {
        this.groupId = null;
        this.tableStatus = TableStatus.STANDBY;
    }

    public void updateStatus(TableStatus status) {
        this.tableStatus = status;
    }

}
