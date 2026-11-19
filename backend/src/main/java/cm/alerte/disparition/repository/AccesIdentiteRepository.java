package cm.alerte.disparition.repository;

import cm.alerte.disparition.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface AccesIdentiteRepository extends JpaRepository<AccesIdentite, Long> {
}
