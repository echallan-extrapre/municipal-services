package org.egov.assets.service;import static org.springframework.util.StringUtils.isEmpty;import java.util.Arrays;import java.util.List;import org.egov.assets.common.DomainService;import org.egov.assets.common.MdmsRepository;import org.egov.assets.common.Pagination;import org.egov.assets.common.exception.CustomBindException;import org.egov.assets.common.exception.ErrorCode;import org.egov.assets.common.exception.InvalidDataException;import org.egov.assets.model.AuditDetails;import org.egov.assets.model.Material;import org.egov.assets.model.MaterialStoreMapping;import org.egov.assets.model.MaterialStoreMappingRequest;import org.egov.assets.model.MaterialStoreMappingResponse;import org.egov.assets.model.MaterialStoreMappingSearch;import org.egov.assets.model.Store;import org.egov.assets.model.StoreGetRequest;import org.egov.assets.repository.MaterialStoreMappingJdbcRepository;import org.egov.assets.repository.entity.MaterialStoreMappingEntity;import org.egov.common.contract.request.RequestInfo;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;import org.springframework.util.StringUtils;@Servicepublic class MaterialStoreMappingService extends DomainService {	public static final String STORE = "store";	public static final String MATERIAL = "material";	@Autowired	private MaterialStoreMappingJdbcRepository materialStoreMappingJdbcRepository;	@Autowired	private StoreService storeService;	@Value("${inv.materialstore.save.topic}")	private String saveTopic;	@Value("${inv.materialstore.save.key}")	private String saveKey;	@Value("${inv.materialstore.update.topic}")	private String updateTopic;	@Value("${inv.materialstore.update.key}")	private String updateKey;	@Value("${inv.materialstore.delete.topic}")	private String deleteTopic;	@Value("${inv.materialstore.delete.key}")	private String deletekey;	@Value("${financial.enabled}")	private boolean isFinancialEnabled;	@Autowired	private MdmsRepository mdmsRepository;	public MaterialStoreMappingResponse create(MaterialStoreMappingRequest materialStoreMappingRequest,			String tenantId) {		try {			List<MaterialStoreMapping> materialStoreMappings = materialStoreMappingRequest.getMaterialStoreMappings();			List<String> sequenceNos = materialStoreMappingJdbcRepository					.getSequence(MaterialStoreMapping.class.getSimpleName(), materialStoreMappings.size());			int i = 0;			for (MaterialStoreMapping materialStoreMapping : materialStoreMappings) {				materialStoreMapping.setId(sequenceNos.get(i));				AuditDetails auditDetails = mapAuditDetails(materialStoreMappingRequest.getRequestInfo());				materialStoreMapping.setAuditDetails(auditDetails);				if (isEmpty(materialStoreMapping.getTenantId())) {					materialStoreMapping.setTenantId(tenantId);				}				validateRequest(materialStoreMapping, tenantId, materialStoreMappingRequest.getRequestInfo());				i++;			}			kafkaQue.send(saveTopic, saveKey, materialStoreMappingRequest);			MaterialStoreMappingResponse materialStoreMappingResponse = new MaterialStoreMappingResponse();			materialStoreMappingResponse.setMaterialStoreMappings(materialStoreMappings);			materialStoreMappingResponse.setResponseInfo(getResponseInfo(materialStoreMappingRequest.getRequestInfo()));			return materialStoreMappingResponse;		} catch (CustomBindException e) {			throw e;		}	}	public MaterialStoreMappingResponse update(MaterialStoreMappingRequest materialStoreMappingRequest,			String tenantId) {		try {			materialStoreMappingRequest.getMaterialStoreMappings().stream().forEach(materialStoreMapping -> {				if (StringUtils.isEmpty((materialStoreMapping.getId()))) {					materialStoreMapping							.setId(materialStoreMappingJdbcRepository.getSequence("seq_materialstoremapping"));				}				AuditDetails auditDetails = mapAuditDetails(materialStoreMappingRequest.getRequestInfo());				materialStoreMapping.setAuditDetails(auditDetails);				if (isEmpty(materialStoreMapping.getTenantId())) {					materialStoreMapping.setTenantId(tenantId);				}				materialStoreMapping.getMaterial().setTenantId(materialStoreMapping.getTenantId());				materialStoreMapping.getMaterial().setAuditDetails(auditDetails);				validateRequest(materialStoreMapping, tenantId, materialStoreMappingRequest.getRequestInfo());			});			kafkaQue.send(saveTopic, saveKey, materialStoreMappingRequest);			MaterialStoreMappingResponse response = new MaterialStoreMappingResponse();			response.setResponseInfo(getResponseInfo(materialStoreMappingRequest.getRequestInfo()));			response.setMaterialStoreMappings(materialStoreMappingRequest.getMaterialStoreMappings());			return response;		} catch (CustomBindException e) {			throw e;		}	}	public MaterialStoreMappingResponse search(MaterialStoreMappingSearch materialStoreMappingSearch,			org.egov.common.contract.request.RequestInfo requestInfo) {		Pagination<MaterialStoreMapping> materialStoreMappingList = materialStoreMappingJdbcRepository				.search(materialStoreMappingSearch);		MaterialStoreMappingResponse response = new MaterialStoreMappingResponse();		response.setMaterialStoreMappings(materialStoreMappingList.getPagedData());		return response;	}	/*	 * private void validateUpdateRequest(MaterialStoreMapping materialStoreMapping,	 * String tenantId) { InvalidDataException errors = new InvalidDataException();	 * setTenantNotPresent(materialStoreMapping, tenantId);	 * 	 * validateChartOfAccount(materialStoreMapping, errors);	 * 	 * if (!isEmpty(materialStoreMapping.getStore().getCode())) {	 * getStore(materialStoreMapping.getStore().getCode(),	 * materialStoreMapping.getTenantId(), errors); } else {	 * errors.addDataError(ErrorCode.OBJECT_NOT_FOUND.getCode(), "Store",	 * materialStoreMapping.getStore().getCode()); }	 * 	 * if (!isEmpty(materialStoreMapping.getMaterial().getCode())) {	 * validateMaterial(materialStoreMapping, tenantId, errors); } else {	 * errors.addDataError(ErrorCode.OBJECT_NOT_FOUND.getCode(), "Material",	 * materialStoreMapping.getMaterial().getCode()); }	 * 	 * if (!isEmpty(materialStoreMapping.getMaterial().getCode()) &&	 * !isEmpty(materialStoreMapping.getStore().getCode())) {	 * uniqueCheck(materialStoreMapping, errors); }	 * 	 * if (errors.getValidationErrors().size() > 0) { throw errors; } }	 */	private void setTenantNotPresent(MaterialStoreMapping materialStoreMapping, String tenantId) {		if (isEmpty(materialStoreMapping.getTenantId())) {			materialStoreMapping.setTenantId(tenantId);		}	}	private void validateRequest(MaterialStoreMapping materialStoreMapping, String tenantId, RequestInfo requestInfo) {		InvalidDataException errors = new InvalidDataException();		setTenantNotPresent(materialStoreMapping, tenantId);		validateChartOfAccount(materialStoreMapping, errors);		if (!isEmpty(materialStoreMapping.getStore().getCode())) {			getStore(materialStoreMapping.getStore().getCode(), materialStoreMapping.getTenantId(), errors);		} else {			errors.addDataError(ErrorCode.MANDATORY_VALUE_MISSING.getCode(), "Store");		}		if (!isEmpty(materialStoreMapping.getMaterial().getCode())				&& !isEmpty(materialStoreMapping.getStore().getCode())) {			uniqueCheck(materialStoreMapping, errors);		}		if (!isEmpty(materialStoreMapping.getMaterial().getCode())) {			validateMaterial(materialStoreMapping, tenantId, errors, requestInfo);		} else {			errors.addDataError(ErrorCode.MANDATORY_VALUE_MISSING.getCode(), "Material Code");		}		if (errors.getValidationErrors().size() > 0) {			throw errors;		}	}	private void validateMaterial(MaterialStoreMapping materialStoreMapping, String tenantId,			InvalidDataException errors, RequestInfo requestInfo) {		Material material = (Material) mdmsRepository.fetchObject(tenantId, "store-asset", "Material", "code",				materialStoreMapping.getMaterial().getCode(), Material.class, requestInfo);		if (null == material) {			errors.addDataError(ErrorCode.OBJECT_NOT_FOUND.getCode(), "Material",					materialStoreMapping.getMaterial().getCode());		}	}	private void validateChartOfAccount(MaterialStoreMapping materialStoreMapping, InvalidDataException errors) {		if (isFinancialEnabled && (null == materialStoreMapping.getChartofAccount()				|| (null != materialStoreMapping.getChartofAccount()						&& isEmpty(materialStoreMapping.getChartofAccount().getGlcode())))) {			errors.addDataError(ErrorCode.NULL_VALUE.getCode(), "Account Code");		}	}	private List<Store> getStore(String storeCode, String tenantId, InvalidDataException errors) {		StoreGetRequest storeGetRequest = getStoreGetRequest(storeCode, tenantId);		List<Store> storeList = storeService.search(storeGetRequest).getStores();		if (storeList.size() == 0) {			errors.addDataError(ErrorCode.INVALID_REF_VALUE.getCode(), "store", storeCode);		}		return storeList;	}	private void uniqueCheck(MaterialStoreMapping materialStoreMapping, InvalidDataException errors) {		if (!materialStoreMappingJdbcRepository.uniqueCheck(MATERIAL, STORE,				new MaterialStoreMappingEntity().toEntity(materialStoreMapping))) {			errors.addDataError(ErrorCode.COMBINATION_EXISTS.getCode(), "Material",					materialStoreMapping.getMaterial().getCode(), "store", materialStoreMapping.getStore().getCode());		}	}	private StoreGetRequest getStoreGetRequest(String storeCode, String tenantId) {		return StoreGetRequest.builder().code(Arrays.asList(storeCode)).tenantId(tenantId).active(true).build();	}}