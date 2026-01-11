package com.example.erp.repository;

import com.example.erp.domain.ProductionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
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
