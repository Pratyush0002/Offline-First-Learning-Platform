package com.studenthelper.server.repository;

import com.studenthelper.server.entity.CrdtOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrdtOperationRepository extends JpaRepository<CrdtOperationEntity, String> {
    List<CrdtOperationEntity> findByNoteIdOrderByLamportClockAscClientIdAsc(String noteId);

    @Query("SELECT o FROM CrdtOperationEntity o WHERE o.clientId = :clientId AND o.lamportClock > :clock ORDER BY o.lamportClock ASC")
    List<CrdtOperationEntity> findByClientIdAndLamportClockGreaterThan(@Param("clientId") String clientId, @Param("clock") long clock);

    List<CrdtOperationEntity> findAllByOrderByLamportClockAscClientIdAsc();
}
