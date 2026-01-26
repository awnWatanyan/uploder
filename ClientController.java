
package com.aeon.acss.fdu.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.aeon.acss.fdu.model.ClientModel;
import com.aeon.acss.fdu.model.dto.ClientDto;
import com.aeon.acss.fdu.service.ClientService;

@Controller
@RequestMapping("/client")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
    }

	// ===== 1) View (Thymeleaf) =====
	@GetMapping
	public String page(Model model) {
		model.addAttribute("pageTitle", "Clients");
		model.addAttribute("content", "client :: content");
		model.addAttribute("activeMenu", "MASTER_CLIENT");
		return "layout/layout";
    }

	// ===== 2) JSON API =====

	// GET /client/api
	@GetMapping("/api")
	@ResponseBody
	public List<ClientDto> list() {
		return clientService.findAllDtos();
    }

	// POST /client/api
	@PostMapping("/api")
	@ResponseBody
	public ClientDto create(@RequestBody ClientDto dto) {
		if (isBlank(dto.getCode()) || isBlank(dto.getService()) || isBlank(dto.getNameTh())
				|| isBlank(dto.getNameEn())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code, service, nameTh, nameEn are required");
		}

		// Duplicate guard on (code, service)
		if (clientService.existsByCodeService(dto.getCode(), dto.getService())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate (code + service)");
		}

		LocalDateTime now = LocalDateTime.now();
		ClientModel c = new ClientModel();
		c.setCode(dto.getCode().trim());
		c.setService(dto.getService().trim());
		c.setNameTh(dto.getNameTh().trim());
		c.setNameEn(dto.getNameEn().trim());

		// TODO: derive from logged-in user instead of trusting input
		c.setCreatedBy(dto.getCreatedBy());
		c.setCreatedAt(now);
		c.setUpdatedBy(dto.getUpdatedBy() != null ? dto.getUpdatedBy() : dto.getCreatedBy());
		c.setUpdatedAt(now);

		ClientModel saved = clientService.save(c);
		return ClientDto.from(saved);
    }

	// PUT /client/api/{id}
	@PutMapping("/api/{id}")
	@ResponseBody
	public ResponseEntity<ClientDto> update(@PathVariable Integer id, @RequestBody ClientDto dto) {
		ClientModel existing = clientService.findById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}

		// We keep code immutable in Edit modal; update service/name fields only.
		if (!isBlank(dto.getService()))
			existing.setService(dto.getService().trim());
		if (!isBlank(dto.getNameTh()))
			existing.setNameTh(dto.getNameTh().trim());
		if (!isBlank(dto.getNameEn()))
			existing.setNameEn(dto.getNameEn().trim());

		existing.setUpdatedBy(dto.getUpdatedBy());
		existing.setUpdatedAt(LocalDateTime.now());

		ClientModel saved = clientService.save(existing);
		return ResponseEntity.ok(ClientDto.from(saved));
	}

	// DELETE /client/api/{id}
	@DeleteMapping("/api/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable Integer id) {
		ClientModel existing = clientService.findById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		clientService.deleteById(id);
		return ResponseEntity.noContent().build();
    }

	// ===== helpers =====
	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
    }
}
