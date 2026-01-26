
package com.aeon.acss.fdu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aeon.acss.fdu.model.ClientModel;

public interface ClientRepository extends JpaRepository<ClientModel, Integer> {
    boolean existsByCodeIgnoreCaseAndServiceIgnoreCase(String code, String service);
}
