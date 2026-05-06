package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AfterSaleActionRequest;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleApplicationCreateRequest;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import com.user.returnsassistant.pojo.AfterSaleEvidence;
import com.user.returnsassistant.pojo.AfterSaleEvidenceRequest;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.UserAccount;

public interface AfterSaleApplicationService {
    PageResult<AfterSaleApplication> page(AfterSaleApplicationSearch search);

    AfterSaleApplication getById(Long id);

    AfterSaleApplication create(AfterSaleApplicationCreateRequest request, UserAccount customer);

    AfterSaleApplication approve(Long id, AfterSaleActionRequest request, UserAccount admin);

    AfterSaleApplication reject(Long id, AfterSaleActionRequest request, UserAccount admin);

    AfterSaleApplication requestEvidence(Long id, AfterSaleActionRequest request, UserAccount admin);

    AfterSaleEvidence addEvidence(Long id, AfterSaleEvidenceRequest request, UserAccount customer);

    ServiceTicket createTicket(Long id, AfterSaleActionRequest request, UserAccount admin);

    void appendTicketProcessLog(Long ticketId, String action, String remark, UserAccount admin);
}
