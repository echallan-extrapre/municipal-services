package org.egov.waterconnection.service;

import java.util.Arrays;

import org.egov.tracer.model.CustomException;
import org.egov.waterconnection.constants.WCConstants;
import org.egov.waterconnection.model.CalculationCriteria;
import org.egov.waterconnection.model.CalculationReq;
import org.egov.waterconnection.model.CalculationRes;
import org.egov.waterconnection.model.Property;
import org.egov.waterconnection.model.WaterConnectionRequest;
import org.egov.waterconnection.repository.ServiceRequestRepository;
import org.egov.waterconnection.util.WaterServicesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CalculationService {

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
    
	@Autowired
	private WaterServicesUtil waterServiceUtil;

	/**
	 * 
	 * @param request
	 * 
	 * If action would be APPROVE_FOR_CONNECTION then
	 * 
	 *Estimate the fee for water application and generate the demand
	 * 
	 */
	public void calculateFeeAndGenerateDemand(WaterConnectionRequest request, Property property) {
		if(WCConstants.ACTION_APPROVE_CONNECTION_CONST.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_APPLY_SECURITY_DEPOSIT.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_APPROVE_FOR_CONNECTION_CONVERSION.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_APPROVE_FOR_CONNECTION_RENAME.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_APPROVE_FOR_CONNECTION_DISCONNECTION.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_SEND_BACK_FOR_ADDON_PAYMENT.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())
				|| WCConstants.ACTION_APPROVE_ACTIVATE_CONNECTION.equalsIgnoreCase(request.getWaterConnection().getProcessInstance().getAction())){
			CalculationCriteria criteria = CalculationCriteria.builder()
					.applicationNo(request.getWaterConnection().getApplicationNo())
					.waterConnection(request.getWaterConnection())
					.tenantId(property.getTenantId()).build();
			CalculationReq calRequest = CalculationReq.builder().calculationCriteria(Arrays.asList(criteria))
					.requestInfo(request.getRequestInfo()).isconnectionCalculation(false).build();
			try {
				log.info("Call to generate demand for application no {}: {}",criteria.getApplicationNo(),mapper.writeValueAsString(calRequest));
				Object response = serviceRequestRepository.fetchResult(waterServiceUtil.getCalculatorURL(), calRequest);
				CalculationRes calResponse = mapper.convertValue(response, CalculationRes.class);
				log.info("Demand response for application no {}: {}",criteria.getApplicationNo(),mapper.writeValueAsString(calResponse));
			} catch (Exception ex) {
				log.error("Calculation response error!!", ex);
				throw new CustomException("WATER_CALCULATION_EXCEPTION", "Calculation response can not parsed!!!");
			}
		}

	}
}