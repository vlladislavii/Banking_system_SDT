package org.example.banking_system_sdt.transaction;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransferValidationConfig {

    @Bean
    public TransferValidationHandler transferValidationChain() {
        TransferValidationHandler h1 = new PositiveAmountHandler();
        TransferValidationHandler h2 = new DifferentAccountsHandler();
        TransferValidationHandler h3 = new AccountExistenceHandler();
        TransferValidationHandler h4 = new AuthorizationHandler();
        TransferValidationHandler h5 = new AccountStatusHandler();
        TransferValidationHandler h6 = new SufficientFundsHandler();
        TransferValidationHandler h7 = new DailyLimitHandler();

        h1.setNext(h2);
        h2.setNext(h3);
        h3.setNext(h4);
        h4.setNext(h5);
        h5.setNext(h6);
        h6.setNext(h7);

        return h1;
    }
}
