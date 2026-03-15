package violet.aigc.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import violet.aigc.common.mapper.AgentMapper;
import violet.aigc.common.pojo.Agent;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.proto_gen.common.BaseResp;
import violet.aigc.common.proto_gen.common.StatusCode;
import violet.aigc.common.service.AgentService;
import violet.aigc.common.utils.SnowFlake;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentServiceImpl implements AgentService {
    @Autowired
    private AgentMapper agentMapper;
    private final SnowFlake agentIdGenerator = new SnowFlake(0, 0);
    private static final int PAGE_SIZE = 20;

    @Override
    public CreateAgentResponse createAgent(CreateAgentRequest req) {
        CreateAgentResponse.Builder resp = CreateAgentResponse.newBuilder();
        Long agentId = agentIdGenerator.nextId();
        Date now = new Date();
        Agent agent = new Agent(null, agentId, req.getAgentName(), req.getAvatarUri(), req.getDescription(), req.getPersonality(), req.getUserId(), now, now, 0, "");
        if(!agentMapper.insertAgent(agent)){
            log.error("[createAgent] insertAgent err, agentId = {}", agentId);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).setAgentId(agentId).build();
    }

    @Override
    public GetAgentsByIdsResponse getAgentsByIds(GetAgentsByIdsRequest req) {
        GetAgentsByIdsResponse.Builder resp = GetAgentsByIdsResponse.newBuilder();
        List<Agent> agents = agentMapper.selectAgentsByIds(req.getAgentIdsList());
        List<AgentInfo> agentInfos = agents.stream().map(Agent::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllAgentInfos(agentInfos).build();
    }

    @Override
    public GetAgentsByUserResponse getAgentsByUser(GetAgentsByUserRequest req) {
        GetAgentsByUserResponse.Builder resp = GetAgentsByUserResponse.newBuilder();
        List<Agent> agents = agentMapper.selectAgentsByOwnerId(req.getUserId(), (req.getPage() - 1) * PAGE_SIZE, PAGE_SIZE);
        List<AgentInfo> agentInfos = agents.stream().map(Agent::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllAgentInfos(agentInfos).build();
    }
}
