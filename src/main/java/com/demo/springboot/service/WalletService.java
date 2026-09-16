package com.demo.springboot.service;

import com.demo.springboot.entity.Wallet;
import com.demo.springboot.repository.WalletRepository;
import com.demo.springboot.request.TransactionRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Optional<Wallet> processTransaction(TransactionRequest request) {
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletId(request.getWalletId());
                    w.setAmount(BigDecimal.ZERO);
                    return w;
                });

        BigDecimal newAmount = switch (request.getOperationType()) {
            case DEPOSIT -> wallet.getAmount().add(request.getAmount());
            case WITHDRAW -> wallet.getAmount().subtract(request.getAmount());
        };

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough funds");
        }

        wallet.setAmount(newAmount);

        walletRepository.save(wallet);

       return walletRepository.findById(wallet.getWalletId());
    }
}
