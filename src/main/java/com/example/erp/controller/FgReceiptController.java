package com.example.erp.controller;

import com.example.erp.controller.dto.FgReceiptListResponse;
import com.example.erp.controller.dto.FgReceiptSaveRequest;
import com.example.erp.controller.dto.FgWarehouseCodeView;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.domain.User;
import com.example.erp.repository.UserRepository;
import com.example.erp.service.FgReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/fg/receipts")
public class FgReceiptController extends PageViewSupport {

    private static final Logger log = LoggerFactory.getLogger(FgReceiptController.class);
    private final FgReceiptService fgReceiptService;
    private final UserRepository userRepository;

    public FgReceiptController(FgReceiptService fgReceiptService, UserRepository userRepository) {
        this.fgReceiptService = fgReceiptService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
    	populate(model, "완제품 입고", "receipt", "pages/fg-receipts", fgReceiptService.findAll());
        return "layout/layout";
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> listReceipts(@RequestParam(name = "styleCode", required = false) String styleCode) {
        try {
            FgReceiptListResponse response = fgReceiptService.findReceiptTargets(styleCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("완제품 입고 조회 중 오류가 발생했습니다.", e);
            String message = "조회 중 오류가 발생했습니다. (원인: " + e.getMessage() + ")";
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    @GetMapping("/warehouses")
    @ResponseBody
    public List<FgWarehouseCodeView> listWarehouses(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return fgReceiptService.findWarehouses(keyword);
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveReceipt(@RequestBody FgReceiptSaveRequest request,
            Principal principal) {
        String empNo = resolveEmpNo(principal);
        try {
            Map<String, Object> result = fgReceiptService.saveInbound(request, empNo);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("완제품 입고 저장 실패. request={}, empNo={}", request, empNo, e);
            String message = buildInboundErrorMessage(e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", message));
        }
    }

    private String resolveEmpNo(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            return null;
        }
        return userRepository.findByUsername(principal.getName()).map(User::getEmpNo).orElse(principal.getName());
    }

    private String buildInboundErrorMessage(Exception e) {
        Throwable root = resolveRootCause(e);
        SQLException sqlException = findSqlException(e);
        StringBuilder detail = new StringBuilder();
        if (root != null && StringUtils.hasText(root.getMessage())) {
            detail.append(root.getMessage());
        } else if (e != null && StringUtils.hasText(e.getMessage())) {
            detail.append(e.getMessage());
        } else {
            detail.append("알 수 없는 오류");
        }
        if (sqlException != null) {
            detail.append(" [SQLState=").append(sqlException.getSQLState()).append(", errorCode=")
                    .append(sqlException.getErrorCode()).append("]");
        }
        return "입고 등록에 실패했습니다. (원인: " + detail + ")";
    }

    private Throwable resolveRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }
}
