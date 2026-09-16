package com.demo.springboot.controller;

import com.demo.springboot.entity.Wallet;
import com.demo.springboot.enums.OperationType;
import com.demo.springboot.repository.WalletRepository;
import com.demo.springboot.request.TransactionRequest;
import com.demo.springboot.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletRepository walletRepository;

    @MockBean
    private WalletService walletService;

    @MockBean
    private LockRegistry lockRegistry;

    private Lock lock;

    @BeforeEach
    void setUp() {
        lock = mock(Lock.class);
        when(lockRegistry.obtain(anyString())).thenReturn(lock);
    }

    // ---------- GET /api/v1/wallet/{id} ----------

    @Test
    void show_returnsWallet_whenExists() throws Exception {
        UUID id = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setWalletId(id);
        wallet.setAmount(new BigDecimal("150.00"));

        when(walletRepository.findById(id)).thenReturn(Optional.of(wallet));

        mockMvc.perform(get("/api/v1/wallet/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(150.00));

        verify(walletRepository).findById(id);
    }

    @Test
    void show_returns404_whenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(walletRepository.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/wallet/{id}", id))
                .andExpect(status().isNotFound());

        verify(walletRepository).findById(id);
    }

    @Test
    void show_returns400_whenIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/v1/wallet/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(walletRepository);
    }

    // ---------- POST /api/v1/wallet ----------

    @Test
    void transaction_deposit_returnsUpdatedWallet() throws Exception {
        UUID id = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(id, OperationType.DEPOSIT, new BigDecimal("100.00"));

        Wallet updated = new Wallet();
        updated.setWalletId(id);
        updated.setAmount(new BigDecimal("100.00"));

        when(walletService.processTransaction(any(TransactionRequest.class))).thenReturn(Optional.of(updated));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(id.toString()))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(walletService).processTransaction(any(TransactionRequest.class));
    }

    @Test
    void transaction_withdraw_returnsUpdatedWallet() throws Exception {
        UUID id = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(id, OperationType.WITHDRAW, new BigDecimal("60.00"));

        Wallet updated = new Wallet();
        updated.setWalletId(id);
        updated.setAmount(new BigDecimal("60.00"));

        when(walletService.processTransaction(any(TransactionRequest.class))).thenReturn(Optional.of(updated));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(60.00));
    }

    @Test
    void transaction_acquiresAndReleasesLock() throws Exception {
        UUID id = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(id, OperationType.DEPOSIT, new BigDecimal("10.00"));

        Wallet updated = new Wallet();
        updated.setWalletId(id);
        updated.setAmount(new BigDecimal("10.00"));
        when(walletService.processTransaction(any())).thenReturn(Optional.of(updated));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(lockRegistry).obtain(id.toString());
        verify(lock).lock();
        verify(lock).unlock();
    }

    @Test
    void transaction_releasesLock_whenServiceThrows() throws Exception {
        UUID id = UUID.randomUUID();
        TransactionRequest request = new TransactionRequest(id, OperationType.WITHDRAW, new BigDecimal("999.00"));

        when(walletService.processTransaction(any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Not enough funds"));

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(lock).lock();
        verify(lock).unlock(); // finally-блок должен снять лок даже при исключении
    }

    @Test
    void transaction_returns415_whenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(walletService);
    }
}