package violet.aigc.common.service;

import violet.aigc.common.proto_gen.aigc.*;

public interface AgentService {
    CreateAgentResponse createAgent(CreateAgentRequest req);

    GetAgentsByIdsResponse getAgentsByIds(GetAgentsByIdsRequest req);

    GetAgentsByUserResponse getAgentsByUser(GetAgentsByUserRequest req);
}
