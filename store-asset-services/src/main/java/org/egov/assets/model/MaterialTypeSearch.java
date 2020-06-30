package org.egov.assets.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class MaterialTypeSearch {

	private String tenantId;

	private List<String> ids;

	private String code;

	private String name;

	private String store;

	private Boolean active = null;

	private Integer pageSize;

	private Integer offSet;

	private String sortBy;

}
