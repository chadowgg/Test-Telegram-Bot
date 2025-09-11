package com.example.test.telegram.bot.entity;

import com.example.test.telegram.bot.enums.Affiliate;
import com.example.test.telegram.bot.enums.Position;
import com.example.test.telegram.bot.enums.RegistrationStatus;
import com.example.test.telegram.bot.enums.Role;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    private Long id;

    @Column(nullable = true, length = 300)
    @Enumerated(EnumType.STRING)
    private Affiliate affiliate;

        @Column(nullable = true, length = 300)
    @Enumerated(EnumType.STRING)
    private Position position;

    @Column(nullable = true, length = 300)
    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus;

    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Role role;
}
