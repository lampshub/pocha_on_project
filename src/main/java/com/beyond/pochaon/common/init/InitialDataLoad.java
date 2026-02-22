package com.beyond.pochaon.common.init;

import com.beyond.pochaon.customerTable.repository.CustomerTableRepository;
import com.beyond.pochaon.owner.repository.OwnerRepository;
import com.beyond.pochaon.store.repository.StoreRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class InitialDataLoad implements CommandLineRunner {
    private final OwnerRepository ownerRepository;
        private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;
    private final CustomerTableRepository customerTableRepository;

    public InitialDataLoad(OwnerRepository ownerRepository, StoreRepository storeRepository, CustomerTableRepository customerTableRepository, PasswordEncoder passwordEncoder) {
        this.ownerRepository = ownerRepository;
        this.storeRepository = storeRepository;
        this.customerTableRepository = customerTableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
    }
}


