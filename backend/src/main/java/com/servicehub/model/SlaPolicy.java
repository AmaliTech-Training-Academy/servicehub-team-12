package com.servicehub.model;

import com.servicehub.model.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "sla_policies")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Priority priority;

    @Column(nullable = false)
    private Integer responseTimeHours;

    @Column(nullable = false)
    private Integer resolutionTimeHours;
}
