package violet.aigc.common.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import violet.aigc.common.pojo.Material;

import java.util.List;

@Mapper
public interface MaterialMapper {
    @Insert("INSERT INTO material VALUES (null,#{materialId},#{materialType},#{userId},#{prompt},#{sourceUrl},#{materialUrl},#{model},#{createTime},#{status},#{extra})" )
    Boolean insertMaterial(Material material);

    @Update("UPDATE material SET material_url = #{materialUrl}, status = #{status} WHERE material_id = #{materialId}" )
    Boolean updateMaterialAfterUpload(Material material);

    @Update("UPDATE material SET status = 4 WHERE material_id = #{materialId}" )
    Boolean deleteMaterial(Long materialId);

    @Select("SELECT * FROM material WHERE user_id = #{userId} AND status != 4 LIMIT #{size} OFFSET #{offset}" )
    List<Material> selectByUserId(Long userId, Integer offset, Integer size);
}
