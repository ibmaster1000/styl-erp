package com.example.erp.repository;

import com.example.erp.domain.ProductionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionAgreementRepository extends JpaRepository<ProductionAgreement, Long> {

	boolean existsByStylesId(Long stylesId);
	
	Optional<ProductionAgreement> findByAgreementCode(String agreementCode);

	@Query(value = """
			select pa.agreement_code as agreementCode,
			       s.style_code as styleCode,
			       coalesce(color_codes.code_name, pa.color_code) as colorName,
			       sum(pa.quantity) as totalQuantity,
			       coalesce(manager_user.name,
			                coalesce(nullif(pa.production_manager, ''), s.product_emp_no)) as productionManagerName
			from production_agreements pa
			join styles s on s.styles_id = pa.styles_id
			left join codes color_codes
			  on color_codes.code_type = pa.color_type
			 and color_codes.code = pa.color_code
			left join users manager_user
			  on manager_user.emp_no = coalesce(nullif(pa.production_manager, ''), s.product_emp_no)
			where (:styleCode is null or s.style_code like concat('%', :styleCode, '%'))
			  and (:agreementCode is null or pa.agreement_code like concat('%', :agreementCode, '%'))
			group by pa.agreement_code, s.style_code, pa.color_code, color_codes.code_name,
			         manager_user.name, s.product_emp_no, pa.production_manager
			order by s.style_code asc, pa.agreement_code asc,
			         coalesce(color_codes.code_name, pa.color_code) asc
			""", countQuery = """
			select count(distinct concat(ifnull(s.style_code, ''), '|', ifnull(pa.agreement_code, ''), '|',
			       ifnull(pa.color_code, '')))
			from production_agreements pa
			join styles s on s.styles_id = pa.styles_id
			where (:styleCode is null or s.style_code like concat('%', :styleCode, '%'))
			  and (:agreementCode is null or pa.agreement_code like concat('%', :agreementCode, '%'))
			""", nativeQuery = true)
	Page<AgreementCodeRowView> findAgreementCodeRows(@Param("styleCode") String styleCode,
			@Param("agreementCode") String agreementCode, Pageable pageable);

	@Query(value = """
			select pa.agreement_code as agreementCode,
			       s.style_code as styleCode,
			       color_codes.code_name as colorName,
			       pa.color_code as colorCode,
			       size_codes.code_name as sizeName,
			       pa.size_code as sizeCode,
			       pa.quantity as quantity,
			       s.supply_price as supplyPrice,
			       (pa.quantity * s.supply_price) as amount,
			       coalesce(nullif(pa.production_manager, ''), s.product_emp_no) as productionManager
			from production_agreements pa
			join styles s on s.styles_id = pa.styles_id
			left join codes color_codes
			  on color_codes.code_type = pa.color_type
			 and color_codes.code = pa.color_code
			left join codes size_codes
			  on size_codes.code_type = pa.size_type
			 and size_codes.code = pa.size_code
			where pa.agreement_code = :agreementCode
			order by pa.color_code, pa.size_code
			""", nativeQuery = true)
	List<AgreementDetailRowView> findAgreementDetailRows(@Param("agreementCode") String agreementCode);

	@Modifying
	@Query("""
			update ProductionAgreement pa
			   set pa.status = :status
			 where pa.agreementCode = :agreementCode
			   and (:colorCode is null or pa.colorCode = :colorCode)
			""")
	int updateStatusByAgreementCodeAndColorCode(@Param("agreementCode") String agreementCode,
			@Param("colorCode") String colorCode, @Param("status") String status);

	@Query(value = """
			select pa.prd_agree_id as prdAgreeId,
			       pa.agreement_code as agreementCode,
			       pa.styles_id as stylesId,
			       pa.style_code as styleCode,
			       pa.production_manager as productionManager,
			       styles.product_emp_no as productEmpNo,
			       users.name as productEmpName,
			       pa.color_type as colorType,
			       pa.color_code as colorCode,
			       color_codes.code_name as colorName,
			       pa.size_type as sizeType,
			       pa.size_code as sizeCode,
			       size_codes.code_name as sizeName,
			       pa.quantity as quantity,
			       pa.status as status,
			       pa.remark as remark
			from production_agreements pa
			left join styles
			  on styles.styles_id = pa.styles_id
			left join users
			  on users.emp_no = styles.product_emp_no
			left join codes color_codes
			  on color_codes.code_type = pa.color_type
			 and color_codes.code = pa.color_code
			left join codes size_codes
			  on size_codes.code_type = pa.size_type
			 and size_codes.code = pa.size_code
			where (:styleCode is null or pa.style_code like concat('%', :styleCode, '%'))
			  and (:agreementCode is null or pa.agreement_code like concat('%', :agreementCode, '%'))
			order by pa.style_code asc, pa.agreement_code asc, pa.color_code asc
			""", nativeQuery = true)
	List<ProductionAgreementView> findAllWithCodeNamesFiltered(@Param("styleCode") String styleCode,
			@Param("agreementCode") String agreementCode);
}
