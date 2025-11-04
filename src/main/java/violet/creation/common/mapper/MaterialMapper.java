package violet.creation.common.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import violet.creation.common.pojo.Material;

import java.util.List;

@Mapper
public interface MaterialMapper {
    @Insert("INSERT INTO material VALUES (null,#{materialId},#{materialType},#{userId},#{prompt},#{sourceUrl},#{materialUrl},#{model},#{createTime},#{status},#{extra})" )
    Boolean insertMaterial(Material material);

    @Update("UPDATE material SET material_url = #{materialUrl}, status = #{status} WHERE material_id = #{materialId}" )
    Boolean updateMaterialAfterUpload(Material material);

    @Select("SELECT * FROM material WHERE user_id = #{userId}" )
    List<Material> selectByUserId(Long userId);
}
