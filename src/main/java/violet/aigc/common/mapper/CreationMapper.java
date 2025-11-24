package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import violet.aigc.common.pojo.Creation;

import java.util.List;

@Mapper
public interface CreationMapper {
    @Insert("INSERT INTO creation VALUES (null,#{creationId},#{userId},#{coverUrl},#{materialId},#{materialType},#{materialUrl},#{title},#{content},#{category},#{createTime},#{modifyTime},#{status},#{extra})")
    Boolean insertCreation(Creation creation);

    @Select("UPDATE creation SET status = 4 WHERE creation_id = #{creationId}")
    Boolean deleteCreation(Long creationId);

    @Select("SELECT * FROM creation WHERE creation_id = #{creationId}")
    Creation selectByCreationId(Long creationId);

    @Select("<script>" +
            "SELECT * FROM creation WHERE creation_id IN " +
            "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Creation> selectByCreationIds(List<Long> creationIds);
}
