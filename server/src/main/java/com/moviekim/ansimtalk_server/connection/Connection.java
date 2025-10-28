package com.moviekim.ansimtalk_server.connection;

import com.moviekim.ansimtalk_server.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_id")
    private User elderly; // 어르신

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private User guardian; // 보호자

    public Connection(User elderly, User guardian) {
        this.elderly = elderly;
        this.guardian = guardian;
    }
}