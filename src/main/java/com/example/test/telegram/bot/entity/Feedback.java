package com.example.test.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="user_feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String feedback;

    @Column(nullable = false)
    private Integer criticality;

    @Column(nullable = false, length = 1000)
    private String recommendation;
}
