package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import violet.aigc.common.pojo.Agent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentMapper {
    @Insert("INSERT INTO agent VALUES (null, #{agentId}, #{agentName}, #{avatarUri}, #{description}, #{personality}, #{ownerId}, #{createTime}, #{modifyTime}, #{status}, #{extra})")
    Boolean insertAgent(Agent agent);

    @Select("<script>" +
            "SELECT * FROM agent WHERE agent_id IN " +
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Agent> selectAgentsByIds(List<Long> agentIds);

    @Select("SELECT * FROM agent WHERE owner_id = #{userId} ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}")
    List<Agent> selectAgentsByOwnerId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);
}
