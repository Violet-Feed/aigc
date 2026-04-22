package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.*;
import violet.aigc.common.pojo.Agent;

import java.util.Date;
import java.util.List;

@Mapper
public interface AgentMapper {
    @Insert("INSERT INTO agent VALUES (null, #{agentId}, #{agentName}, #{avatarUri}, #{description}, #{personality}, #{ownerId}, #{createTime}, #{modifyTime}, #{status}, #{extra})")
    Boolean insertAgent(Agent agent);

    @Delete("DELETE FROM agent WHERE agent_id = #{agentId}")
    Boolean deleteAgent(Long agentId);

    @Update("UPDATE agent SET agent_name = #{agentName}, avatar_uri = #{avatarUri}, description = #{description}, personality = #{personality}, modify_time = #{modifyTime} WHERE agent_id = #{agentId}")
    Boolean updateAgent(@Param("agentId") Long agentId, @Param("agentName") String agentName, @Param("avatarUri") String avatarUri, @Param("description") String description, @Param("personality") String personality, @Param("modifyTime") Date modifyTime);

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
