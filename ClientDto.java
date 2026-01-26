package com.aeon.acss.fdu.model.dto;

import java.time.LocalDateTime;

import com.aeon.acss.fdu.model.ClientModel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // don’t serialize nulls
@JsonIgnoreProperties(ignoreUnknown = true) // ignore extra JSON sent by clients
public class ClientDto {
	private Integer id;
	private String code;
	private String service;
	private String nameTh;
	private String nameEn;
	private Integer createdBy;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;

	private Integer updatedBy;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

	public static ClientDto from(ClientModel c) {
		return ClientDto.builder().id(c.getId()).code(c.getCode()).service(c.getService()).nameTh(c.getNameTh())
				.nameEn(c.getNameEn()).createdBy(c.getCreatedBy()).createdAt(c.getCreatedAt())
				.updatedBy(c.getUpdatedBy()).updatedAt(c.getUpdatedAt()).build();
	}
}
