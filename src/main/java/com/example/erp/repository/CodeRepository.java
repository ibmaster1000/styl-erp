package com.example.erp.repository;

import com.example.erp.domain.Code;
import com.example.erp.domain.CodeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeRepository extends JpaRepository<Code, CodeId> {

    List<Code> findByDeletedFalseOrderByIdGroupCodeAscSortOrderAscIdCodeAsc();

    List<Code> findByIdGroupCodeAndDeletedFalseOrderBySortOrderAscIdCodeAsc(String groupCode);

    boolean existsByIdGroupCodeAndIdCodeAndDeletedFalse(String groupCode, String code);

    Optional<Code> findByIdAndDeletedFalse(CodeId id);

    @Modifying(clearAutomatically = true)
    @Query("update Code c set c.deleted = true where c.id = :id and c.deleted = false")
    int softDelete(@Param("id") CodeId id);
}