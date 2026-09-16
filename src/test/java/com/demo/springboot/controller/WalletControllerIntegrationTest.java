package com.demo.springboot.controller;

import com.demo.springboot.entity.Wallet;
import com.demo.springboot.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletRepository walletRepository;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        walletRepository.deleteAll();
    }

    @Test
    void depositThenWithdraw_keepsCorrectBalance() throws Exception {
        postTransaction(walletId, "DEPOSIT", "100.00");
        Wallet afterDeposit = walletRepository.findById(walletId).orElseThrow();
        assertThat(afterDeposit.getAmount()).isEqualByComparingTo("100.00");

        postTransaction(walletId, "WITHDRAW", "40.00");
        Wallet afterWithdraw = walletRepository.findById(walletId).orElseThrow();
        assertThat(afterWithdraw.getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void depositTwiceAndWithdrawOnce_keepsCorrectBalance() throws Exception {
        postTransaction(walletId, "DEPOSIT", "50.00");
        postTransaction(walletId, "DEPOSIT", "25.00");
        postTransaction(walletId, "WITHDRAW", "15.00");

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getAmount()).isEqualByComparingTo("60.00");
    }

    // Не хватает средств
    @Test
    void withdrawMoreThanBalance_returnsBadRequest_andBalanceUnchanged() throws Exception {
        postTransaction(walletId, "DEPOSIT", "100.00");

        String body = """
                {
                  "walletId": "%s",
                  "operationType": "WITHDRAW",
                  "amount": "150.00"
                }
                """.formatted(walletId);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertThat(wallet.getAmount()).isEqualByComparingTo("100.00");
    }

    private void postTransaction(UUID walletId, String type, String amount) throws Exception {
        String body = """
                {
                  "walletId": "%s",
                  "operationType": "%s",
                  "amount": "%s"
                }
                """.formatted(walletId, type, amount);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
