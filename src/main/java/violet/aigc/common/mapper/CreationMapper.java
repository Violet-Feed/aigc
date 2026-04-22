package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.*;
import violet.aigc.common.pojo.Creation;

import java.util.Date;
import java.util.List;

@Mapper
public interface CreationMapper {
    @Insert("INSERT INTO creation VALUES (null,#{creationId},#{userId},#{coverUrl},#{materialId},#{materialType},#{materialUrl},#{title},#{content},#{category},#{createTime},#{modifyTime},#{status},#{extra})")
    Boolean insertCreation(Creation creation);

    @Update("UPDATE creation SET status = 4 WHERE creation_id = #{creationId}")
    Boolean deleteCreation(Long creationId);

    @Update("UPDATE creation SET title = #{title}, content = #{content}, category = #{category}, modify_time = #{modifyTime} WHERE creation_id = #{creationId}")
    Boolean updateCreation(@Param("creationId") Long creationId, @Param("title") String title, @Param("content") String content, @Param("category") String category, @Param("modifyTime") Date modifyTime);

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
