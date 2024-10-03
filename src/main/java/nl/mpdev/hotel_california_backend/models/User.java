package nl.mpdev.hotel_california_backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "Users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private String userName;
  private String password;
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "remote_id", referencedColumnName = "id")
  private Profile profile;
  @OneToMany(
    targetEntity = Authority.class,
    mappedBy = "username",
    cascade = CascadeType.ALL,
    orphanRemoval = true,
    fetch = FetchType.EAGER
  )
  private Set<Authority> authorities = new HashSet<>();
  @OneToMany(mappedBy = "user")
  private List<Order> orders;
}
