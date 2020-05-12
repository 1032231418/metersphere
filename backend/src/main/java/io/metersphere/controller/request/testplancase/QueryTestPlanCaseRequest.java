package io.metersphere.controller.request.testplancase;

import io.metersphere.base.domain.TestPlanTestCase;
import io.metersphere.controller.request.OrderRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class QueryTestPlanCaseRequest extends TestPlanTestCase {

    private List<String> nodeIds;

    private List<OrderRequest> orders;

    private Map<String, List<String>> filters;

    private String workspaceId;

    private String name;
}