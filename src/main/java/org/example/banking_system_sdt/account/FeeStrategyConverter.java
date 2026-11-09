package org.example.banking_system_sdt.account;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter(autoApply = true)
public class FeeStrategyConverter implements AttributeConverter<FeeStrategy, String> {

    private static final String PREFIX_FIXED = "FIXED:";
    private static final String PREFIX_PERCENTAGE = "PERCENTAGE:";
    private static final String VALUE_NONE = "NONE";

    @Override
    public String convertToDatabaseColumn(FeeStrategy attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute instanceof FixedFeeStrategy fixed) {
            return PREFIX_FIXED + fixed.getFee().toPlainString();
        }
        if (attribute instanceof PercentageFeeStrategy percentage) {
            return PREFIX_PERCENTAGE + percentage.getRate().toPlainString();
        }
        if (attribute instanceof NoFeeStrategy) {
            return VALUE_NONE;
        }
        throw new IllegalArgumentException("Unsupported FeeStrategy type: " + attribute.getClass().getName());
    }

    @Override
    public FeeStrategy convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if (VALUE_NONE.equalsIgnoreCase(dbData.trim())) {
            return new NoFeeStrategy();
        }
        if (dbData.startsWith(PREFIX_FIXED)) {
            String feeStr = dbData.substring(PREFIX_FIXED.length()).trim();
            return new FixedFeeStrategy(new BigDecimal(feeStr));
        }
        if (dbData.startsWith(PREFIX_PERCENTAGE)) {
            String rateStr = dbData.substring(PREFIX_PERCENTAGE.length()).trim();
            return new PercentageFeeStrategy(new BigDecimal(rateStr));
        }
        throw new IllegalArgumentException("Unknown FeeStrategy string: " + dbData);
    }
}
