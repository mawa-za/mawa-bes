package za.co.mawa.bes.xero;

import com.xero.api.ApiClient;
import com.xero.api.client.AccountingApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class XeroConfig {

    @Bean
    public AccountingApi accountingApi() {

        ApiClient apiClient = new ApiClient();

        return new AccountingApi(apiClient);
    }
}