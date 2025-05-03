package Latam.Latam.work.hub.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "IMAGENES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url")
    private String url;


    @ManyToOne
    @JoinColumn(name = "space_id")
    private SpaceEntity space;
}
