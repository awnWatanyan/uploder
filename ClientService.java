
package com.aeon.acss.fdu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aeon.acss.fdu.model.ClientModel;
import com.aeon.acss.fdu.model.dto.ClientDto;
import com.aeon.acss.fdu.repository.ClientRepository;

@Service
public class ClientService {

    private final ClientRepository repo;

    public ClientService(ClientRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ClientDto> findAllDtos() {
        return repo.findAll().stream()
                .map(ClientDto::from)
                .toList();
    }

    @Transactional
	public ClientModel save(ClientModel c) {
        return repo.save(c);
    }

    @Transactional(readOnly = true)
	public ClientModel findById(Integer id) {
        return repo.findById(id).orElse(null);
    }

    @Transactional
    public void deleteById(Integer id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsByCodeService(String code, String service) {
        return repo.existsByCodeIgnoreCaseAndServiceIgnoreCase(code, service);
    }
}
