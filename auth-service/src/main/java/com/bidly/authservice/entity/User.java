package com.bidly.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 30)
    private String lastName;

    @Column(nullable = false, unique = true, length = 25)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role_join",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> rolename;
}
