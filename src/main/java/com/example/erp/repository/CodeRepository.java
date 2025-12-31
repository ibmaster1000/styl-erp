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
	List<Code> findByDeletedFalseAndActiveTrueOrderByIdCodeTypeAscSortOrderAscIdCodeAsc();

	List<Code> findByIdCodeTypeAndDeletedFalseOrderBySortOrderAscIdCodeAsc(String codeType);

	boolean existsByIdCodeTypeAndIdCodeAndDeletedFalse(String codeType, String code);

	Optional<Code> findByIdAndDeletedFalse(CodeId id);

	@Modifying(clearAutomatically = true)
	@Query("update Code c set c.deleted = true where c.id = :id and c.deleted = false")
	int softDelete(@Param("id") CodeId id);
	
	@Query("""
            select c
            from Code c
            where c.deleted = false
              and c.active = true
              and (:codeType is null or c.id.codeType like concat('%', :codeType, '%'))
              and (:code is null or c.id.code like concat('%', :code, '%'))
              and (:codeName is null or c.codeName like concat('%', :codeName, '%'))
              and (:remark is null or c.remark like concat('%', :remark, '%'))
            order by c.sortOrder asc, c.createdAt desc
            """)
    List<Code> searchActiveCodes(@Param("codeType") String codeType,
                                 @Param("code") String code,
                                 @Param("codeName") String codeName,
                                 @Param("remark") String remark);

    @Query("""
            select distinct c.id.codeType
            from Code c
            where c.deleted = false
              and c.active = true
              and c.id.codeType is not null
            order by c.id.codeType asc
            """)
    List<String> findDistinctActiveCodeTypes();
}