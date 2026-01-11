package com.example.erp.repository;

import com.example.erp.domain.ProductionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionAgreementRepository extends JpaRepository<ProductionAgreement, Long> {

	boolean existsByStylesId(Long stylesId);

	@Query(value = """
			select pa.prd_agree_id as prdAgreeId,
			       pa.agreement_code as agreementCode,
			       pa.styles_id as stylesId,
			       pa.style_code as styleCode,
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
			left join codes color_codes
			  on color_codes.code_type = pa.color_type
			 and color_codes.code = pa.color_code
			left join codes size_codes
			  on size_codes.code_type = pa.size_type
			 and size_codes.code = pa.size_code
			""", nativeQuery = true)
	List<ProductionAgreementView> findAllWithCodeNames();
}
