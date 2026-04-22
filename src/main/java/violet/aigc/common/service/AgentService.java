package violet.aigc.common.service;

import violet.aigc.common.proto_gen.aigc.*;

public interface AgentService {
    CreateAgentResponse createAgent(CreateAgentRequest req);

    DeleteAgentResponse deleteAgent(DeleteAgentRequest req);

    UpdateAgentResponse updateAgent(UpdateAgentRequest req);

    GetAgentsByIdsResponse getAgentsByIds(GetAgentsByIdsRequest req);

    GetAgentsByUserResponse getAgentsByUser(GetAgentsByUserRequest req);
}
