package com.servicehub.model;

import com.servicehub.model.enums.RequestCategory;
import com.servicehub.model.enums.RequestPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sla_policies")
@Getter
@Setter
public class SlaPolicy {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RequestPriority priority;

    @Column(name = "response_time_hours", nullable = false)
    private Integer responseTimeHours;

    @Column(name = "resolution_time_hours", nullable = false)
    private Integer resolutionTimeHours;
}
