package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.*;
import violet.aigc.common.pojo.Material;

import java.util.List;

@Mapper
public interface MaterialMapper {
    @Insert("INSERT INTO material VALUES (null,#{materialId},#{materialType},#{userId},#{prompt},#{sourceUrl},#{materialUrl},#{coverUrl},#{model},#{createTime},#{status},#{extra})")
    Boolean insertMaterial(Material material);

    @Update("UPDATE material SET material_url = #{materialUrl}, cover_url = #{coverUrl}, status = #{status} WHERE material_id = #{materialId}")
    Boolean updateMaterialAfterUpload(Material material);

    @Update("UPDATE material SET status = 4 WHERE material_id = #{materialId}")
    Boolean deleteMaterial(Long materialId);

    @Select("SELECT * FROM material WHERE user_id = #{userId} AND status != 4 ORDER BY create_time DESC LIMIT #{size} OFFSET #{offset}")
    List<Material> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("size") Integer size);

    @Select("SELECT * FROM material WHERE material_id = #{materialId}")
    Material selectByMaterialId(Long materialId);
}
