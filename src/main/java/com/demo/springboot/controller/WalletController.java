package com.demo.springboot.controller;

import com.demo.springboot.entity.Wallet;
import com.demo.springboot.enums.OperationType;
import com.demo.springboot.repository.WalletRepository;
import com.demo.springboot.request.TransactionRequest;
import com.demo.springboot.response.WalletResponse;
import com.demo.springboot.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

/**
 * Контроллер для работы с кошельком.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final LockRegistry lockRegistry;

    public WalletController(WalletRepository walletRepository,
                            WalletService walletService,
                            LockRegistry lockRegistry) {
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.lockRegistry = lockRegistry;
    }

    @GetMapping("/{id}")
    public Optional<Wallet> show(@PathVariable UUID id) {
        Optional<Wallet> walletOptional = walletRepository.findById(id);

        if (walletOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }

        return walletOptional;
    }

    @PostMapping("")
    public @ResponseBody Optional<Wallet> transaction(@RequestBody TransactionRequest transactionRequest) {
        Lock lock = lockRegistry.obtain(transactionRequest.getWalletId().toString());
        lock.lock();
        try {
            return walletService.processTransaction(transactionRequest);
        } finally {
            lock.unlock();
        }
    }
}