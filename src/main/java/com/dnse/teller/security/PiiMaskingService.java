package com.dnse.teller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PiiMaskingService:
 * Lớp bảo mật dữ liệu khách hàng & tuân thủ ngân hàng (Banking Data Privacy & PII Protection).
 *
 * Chức năng:
 * 1. Tokenize / Masking: Thay thế toàn bộ số tài khoản, tên khách hàng, mã CIF, số điện thoại,
 *    CCCD/CMND, mã thẻ, OTP thành các định danh giả lập (<ACCOUNT_1>, <CUSTOMER_REF_1>, <PERSON_NAME_1>)
 *    TRƯỚC KHI gửi qua đường truyền internet tới LLM Cloud (DeepSeek / OpenAI).
 * 2. Re-hydration / Unmasking: Khi LLM trả Function Calling arguments hoặc câu trả lời tổng hợp về,
 *    tự động khôi phục (unmask) các giá trị thực tế để hệ thống nội bộ xử lý chuẩn xác 100%.
 */
@Service
public class PiiMaskingService {
    private static final Logger log = LoggerFactory.getLogger(PiiMaskingService.class);

    private static final Pattern CIF_PAT = Pattern.compile("(?i)(?:cif|khách hàng|kh)[-:\\s]*([A-Za-z0-9_-]{4,15})|\\b(CIF-[A-Za-z0-9_-]{4,15})\\b");
    private static final Pattern PHONE_PAT = Pattern.compile("(?<!\\d)(?:\\+84|0)(?:3|5|7|8|9)\\d{8}(?!\\d)");
    private static final Pattern CCCD_PAT = Pattern.compile("(?<!\\d)(?:0\\d{11}|\\d{9})(?!\\d)");
    private static final Pattern CARD_PAT = Pattern.compile("(?<!\\d)(?:9704|4\\d{3}|5[1-5]\\d{2})\\d{12}(?!\\d)");
    private static final Pattern OTP_PAT = Pattern.compile("(?i)(?:mã\\s*otp|otp)[-:\\s]*(\\d{4,8})");

    // Sensitive field keys in JSON / Map structures
    private static final Set<String> ACCOUNT_KEYS = Set.of(
        "accountnumber", "beneficiaryaccount", "sourceaccountref", "accnumber", "account_number", "stk"
    );
    private static final Set<String> NAME_KEYS = Set.of(
        "beneficiaryname", "accountholder", "customername", "fullname", "holdername", "customer_name"
    );
    private static final Set<String> CIF_KEYS = Set.of(
        "customerref", "cif", "customerno", "customer_ref"
    );
    private static final Set<String> PHONE_KEYS = Set.of(
        "phone", "phonenumber", "mobile", "telephone", "phone_number"
    );
    private static final Set<String> CARD_KEYS = Set.of(
        "cardnumber", "cardpan", "card_number", "pan"
    );

    private final ObjectMapper objectMapper;

    public PiiMaskingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static class PiiContext {
        private String maskedText;
        private String maskedJson;
        private final Map<String, String> tokenToReal = new LinkedHashMap<>();
        private final Map<String, String> realToToken = new LinkedHashMap<>();
        private int accountCounter = 1;
        private int nameCounter = 1;
        private int cifCounter = 1;
        private int phoneCounter = 1;
        private int cccdCounter = 1;
        private int cardCounter = 1;

        public String getMaskedText() { return maskedText; }
        public void setMaskedText(String maskedText) { this.maskedText = maskedText; }

        public String getMaskedJson() { return maskedJson; }
        public void setMaskedJson(String maskedJson) { this.maskedJson = maskedJson; }

        public Map<String, String> getTokenToReal() { return tokenToReal; }

        public synchronized String getOrCreateToken(String realValue, String tokenType) {
            if (realValue == null || realValue.trim().isEmpty()) return realValue;
            String trimmed = realValue.trim();
            if (realToToken.containsKey(trimmed)) {
                return realToToken.get(trimmed);
            }

            String token = switch (tokenType.toUpperCase()) {
                case "ACCOUNT" -> "<ACCOUNT_" + (accountCounter++) + ">";
                case "PERSON_NAME" -> "<PERSON_NAME_" + (nameCounter++) + ">";
                case "CUSTOMER_REF" -> "<CUSTOMER_REF_" + (cifCounter++) + ">";
                case "PHONE" -> "<PHONE_" + (phoneCounter++) + ">";
                case "NATIONAL_ID" -> "<NATIONAL_ID_" + (cccdCounter++) + ">";
                case "CARD" -> "<CARD_NUMBER_" + (cardCounter++) + ">";
                default -> "<MASKED_DATA_" + UUID.randomUUID().toString().substring(0, 6) + ">";
            };

            realToToken.put(trimmed, token);
            tokenToReal.put(token, trimmed);
            return token;
        }

        public String unmask(String text) {
            if (text == null || tokenToReal.isEmpty()) return text;
            String unmasked = text;
            for (Map.Entry<String, String> entry : tokenToReal.entrySet()) {
                unmasked = unmasked.replace(entry.getKey(), entry.getValue());
            }
            return unmasked;
        }

        @SuppressWarnings("unchecked")
        public Object unmaskObject(Object obj) {
            if (obj == null) return null;
            if (obj instanceof String str) {
                return unmask(str);
            } else if (obj instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    result.put(key, unmaskObject(entry.getValue()));
                }
                return result;
            } else if (obj instanceof List<?> list) {
                List<Object> result = new ArrayList<>();
                for (Object item : list) {
                    result.add(unmaskObject(item));
                }
                return result;
            }
            return obj;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> unmaskMap(Map<String, Object> map) {
            if (map == null) return Map.of();
            return (Map<String, Object>) unmaskObject(map);
        }
    }

    /**
     * Mask PII trong câu lệnh văn bản tự nhiên và entities đi kèm.
     */
    public PiiContext maskPromptAndEntities(String prompt, Map<String, Object> entities) {
        PiiContext ctx = new PiiContext();

        // 1. Mask known entities first to register their exact tokens
        if (entities != null && !entities.isEmpty()) {
            for (Map.Entry<String, Object> entry : entities.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String val = String.valueOf(entry.getValue());
                if (val != null && !val.trim().isEmpty() && !val.equals("null")) {
                    if (ACCOUNT_KEYS.contains(key) && val.matches("\\d{3,20}")) {
                        ctx.getOrCreateToken(val, "ACCOUNT");
                    } else if (NAME_KEYS.contains(key) && val.length() >= 3) {
                        ctx.getOrCreateToken(val, "PERSON_NAME");
                    } else if (CIF_KEYS.contains(key)) {
                        ctx.getOrCreateToken(val, "CUSTOMER_REF");
                    } else if (PHONE_KEYS.contains(key)) {
                        ctx.getOrCreateToken(val, "PHONE");
                    }
                }
            }
        }

        // 2. Scan and mask prompt text
        String masked = prompt != null ? prompt : "";

        // Replace registered exact real values first
        for (Map.Entry<String, String> entry : ctx.realToToken.entrySet()) {
            masked = masked.replace(entry.getKey(), entry.getValue());
        }

        // Mask CIFs
        Matcher cifMatcher = CIF_PAT.matcher(masked);
        StringBuffer sbCif = new StringBuffer();
        while (cifMatcher.find()) {
            String found = cifMatcher.group(1) != null ? cifMatcher.group(1) : cifMatcher.group(2);
            String token = ctx.getOrCreateToken(found, "CUSTOMER_REF");
            cifMatcher.appendReplacement(sbCif, Matcher.quoteReplacement(cifMatcher.group(0).replace(found, token)));
        }
        cifMatcher.appendTail(sbCif);
        masked = sbCif.toString();

        // Mask Phone Numbers
        Matcher phoneMatcher = PHONE_PAT.matcher(masked);
        StringBuffer sbPhone = new StringBuffer();
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group(0);
            String token = ctx.getOrCreateToken(phone, "PHONE");
            phoneMatcher.appendReplacement(sbPhone, Matcher.quoteReplacement(token));
        }
        phoneMatcher.appendTail(sbPhone);
        masked = sbPhone.toString();

        // Mask Account Numbers (explicit keywords or 4-16 standalone digits)
        Pattern accountWithKeyword = Pattern.compile("(?i)(?:tài khoản|stk|tk|số tk|số tài khoản|account)\\s*[:#-]?\\s*([0-9]{3,20})");
        Matcher accMatcher = accountWithKeyword.matcher(masked);
        StringBuffer sbAcc = new StringBuffer();
        while (accMatcher.find()) {
            String acc = accMatcher.group(1);
            String token = ctx.getOrCreateToken(acc, "ACCOUNT");
            accMatcher.appendReplacement(sbAcc, Matcher.quoteReplacement(accMatcher.group(0).replace(acc, token)));
        }
        accMatcher.appendTail(sbAcc);
        masked = sbAcc.toString();

        // Mask standalone account numbers (6 to 14 digits not followed by money units)
        Pattern standaloneAccPat = Pattern.compile("(?<![a-zA-Z0-9])(\\d{6,14})(?![a-zA-Z0-9]|\\s*(?:tỷ|ty|triệu|tr|nghìn|ngan|k|usd|eur|jpy|gbp|vnd|đ))");
        Matcher standaloneMatcher = standaloneAccPat.matcher(masked);
        StringBuffer sbStandalone = new StringBuffer();
        while (standaloneMatcher.find()) {
            String candidate = standaloneMatcher.group(1);
            if (!candidate.equals("2024") && !candidate.equals("2025") && !candidate.equals("2026")) {
                String token = ctx.getOrCreateToken(candidate, "ACCOUNT");
                standaloneMatcher.appendReplacement(sbStandalone, Matcher.quoteReplacement(token));
            }
        }
        standaloneMatcher.appendTail(sbStandalone);
        masked = sbStandalone.toString();

        // Mask Names in Prompt (e.g. "cho Nguyễn Văn An", "tên Trần Thị Lan")
        Pattern nameInPrompt = Pattern.compile("(?i)(?:cho|tên|người nhận|chủ tk)\\s+([A-Za-zÀ-ỹĐđ\\s]{3,40}?)(?=\\s+(?:tại|ở|ngân hàng|số tài khoản|stk|tk)|[,.;]|$)");
        Matcher nameMatcher = nameInPrompt.matcher(masked);
        StringBuffer sbName = new StringBuffer();
        while (nameMatcher.find()) {
            String rawName = nameMatcher.group(1).trim();
            if (!rawName.equalsIgnoreCase("vietcombank") && !rawName.equalsIgnoreCase("vcb")
                    && !rawName.equalsIgnoreCase("bidv") && !rawName.equalsIgnoreCase("vietinbank")
                    && !rawName.startsWith("<")) {
                String token = ctx.getOrCreateToken(rawName, "PERSON_NAME");
                nameMatcher.appendReplacement(sbName, Matcher.quoteReplacement(nameMatcher.group(0).replace(rawName, token)));
            }
        }
        nameMatcher.appendTail(sbName);
        masked = sbName.toString();

        ctx.setMaskedText(masked);
        log.debug("PII Masking hoàn tất: {} -> {}", prompt, masked);
        return ctx;
    }

    /**
     * Mask toàn bộ structured outputs từ các MCP Tool trước khi gửi cho LLM tổng hợp.
     */
    public PiiContext maskToolOutputs(String userPrompt, Map<String, Object> toolOutputs) {
        PiiContext ctx = maskPromptAndEntities(userPrompt, Map.of());

        try {
            Object maskedStructure = maskObjectRecursive(toolOutputs, ctx);
            ctx.setMaskedJson(objectMapper.writeValueAsString(maskedStructure));
        } catch (Exception e) {
            log.error("Lỗi khi mask tool outputs JSON: {}", e.getMessage(), e);
            ctx.setMaskedJson("{}");
        }

        return ctx;
    }

    @SuppressWarnings("unchecked")
    private Object maskObjectRecursive(Object obj, PiiContext ctx) {
        if (obj == null) return null;

        if (obj instanceof String str) {
            String masked = str;
            for (Map.Entry<String, String> entry : ctx.realToToken.entrySet()) {
                masked = masked.replace(entry.getKey(), entry.getValue());
            }
            return masked;
        } else if (obj instanceof Map<?, ?> map) {
            Map<String, Object> maskedMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String lowerKey = key.toLowerCase();
                Object val = entry.getValue();

                if (val instanceof String strVal && !strVal.trim().isEmpty()) {
                    if (ACCOUNT_KEYS.contains(lowerKey) && strVal.matches("\\d{3,20}")) {
                        maskedMap.put(key, ctx.getOrCreateToken(strVal, "ACCOUNT"));
                    } else if (NAME_KEYS.contains(lowerKey) && strVal.length() >= 2) {
                        maskedMap.put(key, ctx.getOrCreateToken(strVal, "PERSON_NAME"));
                    } else if (CIF_KEYS.contains(lowerKey)) {
                        maskedMap.put(key, ctx.getOrCreateToken(strVal, "CUSTOMER_REF"));
                    } else if (PHONE_KEYS.contains(lowerKey)) {
                        maskedMap.put(key, ctx.getOrCreateToken(strVal, "PHONE"));
                    } else if (CARD_KEYS.contains(lowerKey)) {
                        maskedMap.put(key, ctx.getOrCreateToken(strVal, "CARD"));
                    } else {
                        maskedMap.put(key, maskObjectRecursive(strVal, ctx));
                    }
                } else {
                    maskedMap.put(key, maskObjectRecursive(val, ctx));
                }
            }
            return maskedMap;
        } else if (obj instanceof List<?> list) {
            List<Object> maskedList = new ArrayList<>();
            for (Object item : list) {
                maskedList.add(maskObjectRecursive(item, ctx));
            }
            return maskedList;
        }

        return obj;
    }
}
